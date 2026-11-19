# gsrs-ep-substance-extension
GSRS extension Module for Substances

This is the GSRS 3 extension module for working with IDMP Substances as specified by the ISO 11238 Substance Model.

## Installing the GSRS extension Module

Simply run:
```
./mvnw clean -U install -DskipTests
```

## Modules

### gsrs.module.substance.exporters.XslExporterFactory
The XslExporter can be used for the exporting of the substances using XSL template.

#### Dependencies
* net.sf.saxon.Saxon-HE

#### Configuration

```
ix.ginas.export.exporterfactories.substances.list.FhirR5 = {
    "exporterFactoryClass": "gsrs.module.substance.exporters.XslExporterFactory",
    "order": 3400,
    "parameters": {
        "format": {
            "extension": "fhir.r5.json",
            "displayName": "FHIR R5 JSON File"
        },
        "templateFile": "export-gsrs-fhir-json.xsl",
        "header": "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\": [{\"resource\":",
        "footer": "}}]}",
        "delimiter": "},{\"resource\":",
        "shouldCompress": false
    }
}
ix.ginas.export.exporterfactories.substances.list.Gsrsp = {
    "exporterFactoryClass": "gsrs.module.substance.exporters.XslExporterFactory",
    "order": 2100,
    "parameters": {
        "format": {
            "extension": "gsrsp",
            "displayName": "GSRS portable, gzipped JSON (.gsrs)"
        },
        "templateFile": "export-gsrsp.xsl",
        "header": "\t\t",
        "footer": "",
        "delimiter": "\n\t\t",
        "shouldCompress": true
    }
}
```

#### Additional XSL template parameters

Both factories support the optional `xslParameters` property, a name/value map of
extra parameters set on the XSL transformer in addition to the ones provided by
the adapter itself (`input` for the exporter; `input` and `input-format` for the
importer). Values are passed as strings. Parameter names can be plain (`name`),
prefixed (`prefix:name`), Clark (`{uri}name`) or EQName (`Q{uri}name`) notation.
Prefixed names require the corresponding mapping in the optional `xslNamespaces`
property.

The parameters can be supplied in three places:

| Place | Exporter | Importer |
|---|---|---|
| GSRS configuration file | `parameters` block of the factory entry | `parameters` block of the factory entry |
| Per request | detailed parameters of the export request | adapter settings of the import job |
| Built-in (always set by the adapter) | `input` | `input`, `input-format` |

Configuration values form the base, request-time values override them, and the
built-in parameters are applied last so they always win.

##### Exporter example

```
ix.ginas.export.exporterfactories.substances.list.FhirR5 = {
    "exporterFactoryClass": "gsrs.module.substance.exporters.XslExporterFactory",
    "order": 3400,
    "parameters": {
        "format": {
            "extension": "fhir.r5.json",
            "displayName": "FHIR R5 JSON File"
        },
        "templateFile": "export-gsrs-fhir-json.xsl",
        "header": "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\": [{\"resource\":",
        "footer": "}}]}",
        "delimiter": "},{\"resource\":",
        "shouldCompress": false,
        "xslParameters": {
            "language": "en",
            "emit-ids": "true",
            "idmp:version": "2"
        },
        "xslNamespaces": {
            "idmp": "urn:gsrs:idmp"
        }
    }
}
```

The same values can be overridden for a single export request by sending them in
the request's detailed parameters:

```
{
    "format": "fhir.r5.json",
    "xslParameters": {
        "language": "de"
    }
}
```

`xslParameters` and `xslNamespaces` are published in the exporter's parameter
schema, so they are rendered as free-form key/value fields in the GSRS export
dialog.

##### GSRSP exporter example: setting `code-system`

The `export-gsrsp.xsl` template adds a provenance entry to every exported
substance. Its `citation` names the code system the portable document is written
for, and it comes from the `code-system` parameter, which defaults to `FDA UNII`:

```xml
<xsl:param name="code-system" as="xs:string" select="'FDA UNII'"/>
```

A registry that records another identifier system sets the parameter instead of
editing the stylesheet. In the GSRS configuration file:

```
ix.ginas.export.exporterfactories.substances.list.Gsrsp = {
    "exporterFactoryClass": "gsrs.module.substance.exporters.XslExporterFactory",
    "order": 2100,
    "parameters": {
        "format": {
            "extension": "gsrsp",
            "displayName": "GSRS portable, gzipped JSON (.gsrs)"
        },
        "templateFile": "export-gsrsp.xsl",
        "header": "\t\t",
        "footer": "",
        "delimiter": "\n\t\t",
        "shouldCompress": true,
        "xslParameters": {
            "code-system": "EMA SMS"
        }
    }
}
```

`code-system` is a plain name, so no `xslNamespaces` entry is needed. For a
single export the same override goes in the request's detailed parameters:

```
{
    "format": "gsrsp",
    "xslParameters": {
        "code-system": "EMA SMS"
    }
}
```

Because the parameter has a default in the stylesheet, an export that does not
set it still succeeds and is cited as `FDA UNII`.

##### Importer example

```
gsrs.importAdapterFactories.substances.list.FhirR5ImportAdapterFactory = {
    "adapterName": "GSRS FHIR R5 JSON Adapter",
    "importAdapterFactoryClass": "gsrs.module.substance.importers.XslImportAdapterFactory",
    "stagingAreaServiceClass": "gsrs.stagingarea.service.DefaultStagingAreaService",
    "entityServiceClass" :"gsrs.dataexchange.SubstanceStagingAreaEntityService",
    "description" : "GSRS FHIR R5 JSON file importer",
    "supportedFileExtensions": [
        "json"
    ],
    "parameters": {
        "templateFile": "import-gsrs-fhir-json.xsl",
        "header": "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\": [{\"resource\":",
        "footer": "}}]}",
        "delimiter": "},{\"resource\":",
        "xslParameters": {
            "language": "en",
            "emit-ids": "true",
            "idmp:version": "2"
        },
        "xslNamespaces": {
            "idmp": "urn:gsrs:idmp"
        }
    }
}
```

For the importer the same values can be supplied per import job in the adapter
settings, either nested under `parameters` or directly on the settings node:

```
{
    "parameters": {
        "templateFile": "import-gsrs-fhir-json.xsl",
        "xslParameters": {
            "language": "de"
        }
    }
}
```


### gsrs.module.substance.importers.XslImportAdapterFactory
The XslImporteAdapter can be used for the importing of the substances using XSL template.

#### Dependencies
* net.sf.saxon.Saxon-HE

#### Configuration

```
gsrs.importAdapterFactories.substances.list.GSRSPImportAdapterFactory = {
    "adapterName": "GSRSP JSON Adapter",
    "importAdapterFactoryClass": "gsrs.module.substance.importers.XslImportAdapterFactory",
    "stagingAreaServiceClass": "gsrs.stagingarea.service.DefaultStagingAreaService",
    "entityServiceClass" :"gsrs.dataexchange.SubstanceStagingAreaEntityService",
    "description" : "Portable GSRSP JSON file importer",
    "supportedFileExtensions": [
        "gsrsp",
        "gz"
    ],
    "parameters": {
        "templateFile": "import-gsrsp.xsl",
        "delimiter": "\n\t\t"
    }
}

gsrs.importAdapterFactories.substances.list.FhirR5ImportAdapterFactory = {
    "adapterName": "GSRS FHIR R5 JSON Adapter",
    "importAdapterFactoryClass": "gsrs.module.substance.importers.XslImportAdapterFactory",
    "stagingAreaServiceClass": "gsrs.stagingarea.service.DefaultStagingAreaService",
    "entityServiceClass" :"gsrs.dataexchange.SubstanceStagingAreaEntityService",
    "description" : "GSRS FHIR R5 JSON file importer",
    "supportedFileExtensions": [
        "json"
    ],
    "parameters": {
        "templateFile": "import-gsrs-fhir-json.xsl",
        "header": "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\": [{\"resource\":",
        "footer": "}}]}",
        "delimiter": "},{\"resource\":"
    }
}
```

### gsrs.module.substance.processors.SubstanceReferenceProcessor
The SubstanceReferenceProcessor can be used to fix broken substance references after substances import from external GSRS system.

#### Configuration

```
gsrs.entityProcessors.list.SubstanceReferenceProcessor = {
    "entityClassName" = "ix.ginas.models.v1.SubstanceReference",
    "processor" = "gsrs.module.substance.processors.SubstanceReferenceProcessor",
    "order" = 2400,
    "with" = {
        "codeSystemPatterns" : [
            {"pattern": "^[0-9A-Z]{10}$", "codeSystem": "FDA UNII"}
        ]
    }
}
```

### gsrs.module.substance.tasks.UpdateEntityTaskInitializer
The UpdateEntityTaskInitializer task can be used for updating attributes from any Entity class in the GSRS
The optional parameter "query" can be used for granular selection of the objects.
The optional parameter "resetFields" can be used to nullify specified fields before invoking the "preUpdate" method.

#### Configuration

```
gsrs.scheduled-tasks.list.UpdateEntityTaskInitializer = {
    "scheduledTaskClass" : "gsrs.module.substance.tasks.UpdateEntityTaskInitializer",
    "order": 2000,
    "parameters" : {
        "entityClass": "ix.ginas.models.v1.Code",
        "query": "select uuid from Code where codeSystem = 'CAS'",
        "resetFields": ["url"],
        "autorun": false
    }
}
```

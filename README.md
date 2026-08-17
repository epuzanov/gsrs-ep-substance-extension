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
ix.ginas.export.exporterfactories.substances.list.EmaSmsFhir = {
    "exporterFactoryClass": "gsrs.module.substance.exporters.XslExporterFactory",
    "order": 2000,
    "parameters": {
        "format": {
            "extension": "ema.hfir.json.gz",
            "displayName": "EMA SMS FHIR JSON File"
        },
        "templateFile": "ema-fhir-json.xsl",
        "header": "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\": [{\"resource\":",
        "footer": "]}",
        "delimiter": "},{\"resource\":",
        "shouldCompress": true
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
        "templateFile": "gsrsp.xsl",
        "header": "\t\t",
        "footer": "",
        "delimiter": "\n\t\t",
        "shouldCompress": true
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

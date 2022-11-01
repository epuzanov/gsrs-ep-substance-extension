# gsrs-ep-substance-extension
GSRS extension Module for Substances

This is the GSRS 3 extension module for working with IDMP Substances as specified by the ISO 11238 Substance Model.

## Installing the GSRS extension Module

Simply run:
```
./mvnw clean -U install -DskipTests
```

## Modules

### gsrs.module.substance.exporters.GsrsApiExporterFactory
The GsrsApiExporter can be used for the exporting of the substances directly to anothe GSRS instance.

#### Dependencies
* org.apache.httpcomponents.httpclient

#### Configuration

```
ix.ginas.export.exporterfactories.substances += {
    "exporterFactoryClass": "gsrs.module.substance.exporters.GsrsApiExporterFactory",
    "parameters": {
        "format": {
            "extension": "gsrsapi",
            "displayName": "Send to ..."
        },
        "headers": {
            "auth-username": "admin",
            "auth-password": "admin"
        },
        "baseUrl": "https://public.gsrs.test/api/v1/substances",
        "timeout": 120000,
        "trustAllCerts": false,
        "validate": true
    }
}
```

### gsrs.module.substance.exporters.JmespathSpreadsheetExporterFactory
The JmespathSpreadsheetExporter can to be used for exporting substances to Excel file with custom defined fields. It uses Jmespath expressions to select values from substances json.

#### Dependencies
* io.burt.jmespath-jackson

#### Configuration

```
ix.ginas.export.exporterfactories.substances += {
    "exporterFactoryClass": "gsrs.module.substance.exporters.JmespathSpreadsheetExporterFactory",
    "parameters": {
        "format": {
            "extension": "custom.xlsx",
            "displayName": "Custom Report (xlsx) File"
        },
        "columnExpressions": [
            {"name":"UUID", "expression":"uuid"},
            {"name":"NAME", "expression":"_name"},
            {"name":"APPROVAL_ID", "expression":"_approvalIDDisplay"},
            {"name":"SMILES", "expression":"structure.smiles"},
            {"name":"FORMULA", "expression":"structure.formula"},
            {"name":"SUBSTANCE_TYPE", "expression":"substanceClass"},
            {"name":"STD_INCHIKEY", "expression":"structure.inchikey"},
            {"name":"STD_INCHIKEY_FORMATTED", "expression":"structure.inchikeyf"},
            {"name":"CAS", "expression":"codes[?codeSystem=='CAS'].code","delimiter":"|"},
            {"name":"EC", "expression":"codes[?codeSystem=='ECHA (EC/EINECS)'].code"},
            {"name":"ITIS", "expression":"codes[?codeSystem=='ITIS'].code"},
            {"name":"NCBI", "expression":"codes[?codeSystem=='NCBI TAXONOMY'].code"},
            {"name":"USDA_PLANTS", "expression":"codes[?codeSystem=='USDA PLANTS'].code"},
            {"name":"INN", "expression":"codes[?codeSystem=='INN'].code"},
            {"name":"NCI_THESAURUS", "expression":"codes[?codeSystem=='NCI_THESAURUS'].code"},
            {"name":"RXCUI", "expression":"codes[?codeSystem=='RXCUI'].code"},
            {"name":"PUBCHEM", "expression":"codes[?codeSystem=='PUBCHEM'].code"},
            {"name":"MPNS", "expression":"codes[?codeSystem=='MPNS'].code"},
            {"name":"GRIN", "expression":"codes[?codeSystem=='GRIN'].code"},
            {"name":"INGREDIENT_TYPE", "expression":"relationships[?contains(['IONIC MOIETY', 'MOLECULAR FRAGMENT', 'UNSPECIFIED INGREDIENT', 'SPECIFIED SUBSTANCE'], type)].type || 'INGREDIENT SUBSTANCE'"},
            {"name":"PROTEIN_SEQUENCE", "expression":"protein.subunits[].sequence","delimiter":"|"},
            {"name":"NUCLEIC_ACID_SEQUENCE", "expression":"nucleicAcid.subunits[].sequence","delimiter":"|"},
            {"name":"RECORD_ACCESS_GROUPS", "expression":"access","delimiter":"|"}
        ]
    }
}
```

### gsrs.module.substance.exporters.JsonPortableExporterFactory
The JsonPortableExporter can be used for exporting substances in the GSRS portable format.

#### Dependencies
* org.apache.cxf.cxf-rt-rs-security-jose

#### Configuration

```
ix.ginas.export.exporterfactories.substances += {
    "exporterFactoryClass": "gsrs.module.substance.exporters.GsrsApiExporterFactory",
    "parameters": {
        "format": {
            "extension": "gsrsp",
            "displayName": "Json Portable Export (gsrsp) File"
        },
        "gsrsVersion": "3.0.2",
        "sign": false,
        "shouldCompress": true
    }
}
```

### gsrs.module.substance.indexers.JmespathIndexvalueMaker
The JmespathIndexvalueMaker canbe used for creating of the custom indexes. It uses Jmespath expressions to select values from substances json.

#### Dependencies
* io.burt.jmespath-jackson

#### Configuration

```
gsrs.indexers.list += {
    "class" = "ix.ginas.models.v1.Substance",
    "indexer" = "gsrs.module.substance.indexers.JmespathIndexValueMaker",
    "parameters" = {
        "regex" = "MyCategory.(.*)",
        "expressions" = [
            "codes[?codeSystem=='MyCategory' && starts_with(comments, 'MyCatogory|')].comments.{\"My Categories\": @}"
        ]
    }
}
```

### gsrs.module.substance.processors.CVClassificationsCodeProcessor
The CVClassificationsCodeProcessor can be used for creating the comment string for classification codes.

#### Configuration

```
gsrs.entityProcessors += {
    "entityClassName" = "ix.ginas.models.v1.Code",
    "processor" = "gsrs.module.substance.processors.CVClassificationsCodeProcessor",
    "with" = {
        "codeSystem" = "WHO-ATC",
        "prefix" = "ATC",
        "masks" = [1, 3, 4, 5],
        "terms" = {
            "C" = "Cardiovascular system",
            "C01" = "Cardiac therapy",
            "C01E" = "Other cardiac preparations",
            "C01EB" = "Other cardiac preparations",
            "C01EB16" = "Ibuprofen",
            "G" = "Genito urinary system and sex hormones",
            "G02" = "Other gynecologicals",
            "G02C" = "Other gynecologicals",
            "G02CC" = "Antiinflammatory products for vaginal administration",
            "G02CC01" = "Ibuprofen",
            "M" = "Musculo-skeletal system",
            "M01" = "Antiinflammatory and antirheumatic products",
            "M01A" = "Antiinflammatory and antirheumatic products, non-steroids",
            "M01AE" = "Propionic acid derivatives",
            "M01AE01" = "Ibuprofen",
            "M01AE51" = "Ibuprofen, combinations",
            "N" = "Nervous system",
            "N02" = "Analgesics",
            "N02A" = "Opioids",
            "N02AJ" = "Opioids in combination with non-opioid analgesics",
            "N02AJ08" = "Codeine and Ibuprofen",
            "N02AJ19" = "Oxycodone and Ibuprofen",
            "N04" = "Anti-parkinson",
            "N04B" = "Dopaminergic agents",
            "N04BC" = "Dopamine agonists",
            "N04BC05" = "Pramipexole"
        }
    }
}
```

#### Alternative Configuration 1
Include Veterinary ATC codes dictionary from JSON file include LEVEL 5 codes.

```
gsrs.entityProcessors += {
    "entityClassName" = "ix.ginas.models.v1.Code",
    "processor" = "gsrs.module.substance.processors.CVClassificationsCodeProcessor",
    "with" = {
        "codeSystem" = "WHO-VATC",
        "prefix" = "VATC",
        "masks" = [2, 4, 5, 6, 8],
        "terms" = { include "vatcCodes.json" }
    }
}
```

#### Alternative Configuration 2
Use the GSRS CV for storing ATC Classification information. And initially populate the CV from the JSON file if the cvVersion is greater then the version of the CV Domain.

```
gsrs.entityProcessors += {
    "entityClassName" = "ix.ginas.models.v1.Code",
    "processor" = "gsrs.module.substance.processors.CVClassificationsCodeProcessor",
    "with" = {
        "codeSystem" = "WHO-ATC",
        "prefix" = "ATC",
        "masks" = [1, 3, 4, 5],
        "cvDomain": "CLASSIFICATION_WHO_ATC",
        "cvVersion": 2,
        "terms" = { include "atcCodes.json" }
    }
}
```

### gsrs.module.substance.processors.SubstanceReferenceProcessor
The SubstanceReferenceProcessor canbe used to fix broken substance references after substances import from external GSRS system.

#### Configuration

```
gsrs.entityProcessors += {
    "entityClassName" = "ix.ginas.models.v1.SubstanceReference",
    "processor" = "gsrs.module.substance.processors.SubstanceReferenceProcessor",
    "with" = {
        "codeSystemPatterns" : [
            {"pattern": "^[0-9A-Z]{10}$", "codeSystem": "FDA UNII"}
        ]
    }
}
```

### gsrs.module.substance.tasks.UpdateCodeTaskInitializer
The UpdateCodeTaskInitializer task can be used for updating All Codes attributes (comments, url, access) in the GSRS

#### Configuration

```
gsrs.scheduled-tasks.list+= {
    "scheduledTaskClass" : "gsrs.module.substance.tasks.UpdateCodeTaskInitializer",
    "parameters" : {
        "autorun": false
    }
}
```

### gsrs.module.substance.tasks.UpdateSubstanceReferenceTaskInitializer
The SubstanceReferenceProcessor canbe used to fix broken substance references after substances import from external GSRS system.

#### Configuration

```
gsrs.scheduled-tasks.list+= {
    "scheduledTaskClass" : "gsrs.module.substance.tasks.UpdateSubstanceReferenceTaskInitializer",
    "parameters" : {
        "autorun": false
    }
}
```

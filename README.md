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
            #"auth-username": "admin",
            #"auth-password": "admin",
            #"AUTHENTICATION_HEADER_NAME_EMAIL": "{{user.email}}",
            "auth-username": "{{user.name}}",
            "auth-key": "{{user.apikey}}"

        },
        "baseUrl": "https://public.gsrs.test/api/v1/substances",
        "timeout": 120000,
        "trustAllCerts": false,
        "allowedRole": "Approver",
        "newAuditor": "admin",
        "changeReason": "{{changeReason}} (Version {{version}})",
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
            {"name":"PROTEIN_SEQUENCE", "expression":"protein.subunits[].sequence", "delimiter":"|"},
            {"name":"NUCLEIC_ACID_SEQUENCE", "expression":"nucleicAcid.subunits[].sequence", "delimiter":"|"},
            {"name":"RECORD_ACCESS_GROUPS", "expression":"access", "delimiter":"|"},
            {"name":"LAST_EDITED", "expression":"lastEdited", "datetime":"yyyy-MM-dd HH:mm:ss"}
        ]
    }
}
```

### gsrs.module.substance.indexers.JmespathIndexValueMaker
The JmespathIndexvalueMaker canbe used for creating of the custom indexes. It uses Jmespath expressions to select values from substances json.

#### Dependencies
* io.burt.jmespath-jackson

#### Configuration

```
gsrs.indexers.list += {
    "class" = "ix.ginas.models.v1.Substance",
    "indexer" = "gsrs.module.substance.indexers.JmespathIndexValueMaker",
    "parameters" = {
        "expressions" = [
            {"index":"ATC Level 1", "expression": "codes[?codeSystem=='WHO-ATC' && starts_with(comments, 'ATC|')].comments", "regex":"ATC.([^\\Q|\\E]*).*"},
            {"index":"ATC Level 2", "expression": "codes[?codeSystem=='WHO-ATC' && starts_with(comments, 'ATC|')].comments", "regex":"ATC.[^\\Q|\\E]*.([^\\Q|\\E]*).*"},
            {"index":"ATC Level 3", "expression": "codes[?codeSystem=='WHO-ATC' && starts_with(comments, 'ATC|')].comments", "regex":"ATC.[^\\Q|\\E]*.[^\\Q|\\E]*.([^\\Q|\\E]*).*"},
            {"index":"ATC Level 4", "expression": "codes[?codeSystem=='WHO-ATC' && starts_with(comments, 'ATC|')].comments", "regex":"ATC.[^\\Q|\\E]*.[^\\Q|\\E]*.[^\\Q|\\E]*.([^\\Q|\\E]*).*"},
            {"index":"Naming Orgs", "expression": "names[?type=='of'].nameOrgs[]"},
            {"index":"Name TypeLang", "expression": "names[?type=='of'].languages[].join('_', ['of', @])"},
            {"index":"Name TypeLang", "expression": "names[?type=='sys'].languages[].join('_', ['sys', @])"},
            {"index":"Name TypeLang", "expression": "names[?type=='cn'].languages[].join('_', ['cn', @])"},
            {"index":"Name TypeLang", "expression": "names[?type=='bn'].languages[].join('_', ['bn', @])"},
            {"index":"Name TypeLang", "expression": "names[?type=='cd'].languages[].join('_', ['od', @])"},
            {"index":"Reference Tags", "expression": "references[].tags[]"},
            {"index":"Molecular Weight", "expression": "properties[?starts_with(name, 'MOL_WEIGHT')].floor(value.average)", "ranges": "0 200 400 600 800 1000", "format": "%1$.0f", "sortable":true},
            {"index":"root_structure_mwt", "type": "Double", "expression": "properties[?starts_with(name, 'MOL_WEIGHT')].value.average", "sortable":true},
            {"index":"Deprecated", "expression": "[map(&'Deprecated',[deprecated][?@]),'Not Deprecated'][] | @[0]"}
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

### gsrs.module.substance.processors.DBClassificationsCodeProcessor
The DBClassificationsCodeProcessor can be used for creating the comment string for classification codes using SQL database as the source. The query must return 4 fields.
The first field contains COMMENTS text, the second field contains URL, the third field contains DOC_TYPE of the Reference and the fourth field contains CITATION of the Reference.
The second, third and fourth fields can return NULL values.

#### Configuration

```
gsrs.entityProcessors += {
    "entityClassName" = "ix.ginas.models.v1.Code",
    "processor" = "gsrs.module.substance.processors.DBClassificationsCodeProcessor",
    "with" = {
        "codeSystem" = "PV",
        "query" = """SELECT
'ROOT|' || SUB_CATEGORY || '|' || CLASSIFICATION,
URL,
REF_DOC_TYPE,
REF_CITATION
FROM CLASSIFICATIONS
WHERE CODE = ?
""",
        "datasource" = {
            "url" = "jdbc:oracle:thin:@//db-server:1521/CLASSIFICATIONS",
            "username" = "gsrs",
            "password" = "somepassword"
        }
    }
}
```

### gsrs.module.substance.processors.SubstanceReferenceProcessor
The SubstanceReferenceProcessor can be used to fix broken substance references after substances import from external GSRS system.

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

### gsrs.module.substance.processors.SetAccessCodeProcessor
The SetAccessCodeProcessor can be used to force the access value for the specific code system.

#### Configuration

```
gsrs.entityProcessors += {
    "class":"ix.ginas.models.v1.Code",
    "processor":"gsrs.module.substance.processors.SetAccessCodeProcessor",
    "with":{
        "codeSystemAccess": {
            "BDNUM": ["protected"],
            "*": []
        }
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
The SubstanceReferenceProcessor can be used to fix broken substance references after substances import from external GSRS system.

#### Configuration

```
gsrs.scheduled-tasks.list+= {
    "scheduledTaskClass" : "gsrs.module.substance.tasks.UpdateSubstanceReferenceTaskInitializer",
    "parameters" : {
        "autorun": false
    }
}
```

### gsrs.module.substance.tasks.ScheduledSQLExportTask
The ScheduledSQLExportTask can be used for dump large amount of information from GSRS database into CSV files.
Multiple CSV files can be created, compressed (optional) and sent to the multiple destinations. Following
destinations protocols are supported: file://, ftp://, ftps://, sftp://.
If the returned string value need to be additionally converted to some custom format,
then fields name in the SQL query must be specified in the following form "CAST_TO_<FORMAT>_<FIELDS_NAME>".
The custom strings format support must be implemented in the toFormat method of the custom StringConverted class.

#### Dependencies
* org.apache.commons.vfs2
* org.apache.commons.compress
* org.apache.commons.net
* com.jcraft.jsch

#### Configuration

```
gsrs.scheduled-tasks.list+= {
    "scheduledTaskClass" : "gsrs.module.substance.tasks.ScheduledSQLExportTask",
    "parameters" : {
        "cron":"0 0 0 * * ?",
        "autorun":false,
        "name":"Target System Name",
        "stringConverter": "gsrs.module.substance.converters.DefaultStringConverter",
        "destinations": [
            {
                "uri": "file:///home/srs/exports/Names.zip"
            },
            {
                "uri":"sftp://target_server/inbox/Names.tar.gz",
                "user":"username",
                "password":"PASSWORD",
                "userDirIsRoot":"false",
                "strictHostKeyChecking":"no",
                "sessionTimeoutMillis":"10000"
            },
            {
                "uri":"ftp://target_server_2/gsrs_exports/",
                "user":"username",
                "password":"PASSWORD",
                "userDirIsRoot":"true",
                "pasiveMode":"true",
            }
        ],
        "files": [
            {
                "name":"Names.csv",
                "msg":"Display Names",
                "encoding":"ISO-8859-1",
                "delimiter":";",
                "quoteChar":"\"",
                "escapeChar":"",
                "header":"UNII;NAME",
                "sql":"""
SELECT
    S.APPROVAL_ID AS UNII,
    COALESCE(N.FULL_NAME, N.NAME) AS NAME
FROM IX_GINAS_SUBSTANCE S
LEFT JOIN IX_GINAS_NAME N ON (S.UUID = N.OWNER_UUID)
WHERE S.DEPRECATED = '0'
    AND N.DISPLAY_NAME = '1'
"""
            }
        ]
    }
}
```

### ix.ginas.utils.validation.validators.JmespathValidator
The JmespathValidator can to be used for creating the custom GSRS validations rules. It uses Jmespath expressions to validate the substances json.

#### Dependencies
* io.burt.jmespath-jackson

#### Configuration

```
gsrs.validators.substances += {
    "validatorClass" = "ix.ginas.utils.validation.validators.JmespathValidator",
    "newObjClass" = "ix.ginas.models.v1.Substance",
    "configClass" = "SubstanceValidatorConfig",
    "parameters"= {
        "expressions" = [
            {"messageType": "ERROR", "messageTemplate": "Only single %s Code allowed.", "expression": "new.codes[?type=='PRIMARY' && codeSystem=='MyCodeSystem'].codeSystem | [1]"},
            {"messageType": "ERROR", "messageTemplate": "The MyCodeSystem Code can not be changed.", "expression": "values(@)[*].codes[?type=='PRIMARY' && codeSystem=='MyCodeSystem'].code | [] | [0] != [1] && [1] != `null`"},
            {"messageType": "ERROR", "messageTemplate": "More then One Note with Public Reference is not allowed.", "expression": "new.notes[?length(references[? publicDomain==`true` && contains(tags, 'PUBLIC_DOMAIN_RELEASE') && length(access) == `0`]) > `0`].note | [1]"}.
            {"messageType": "ERROR", "messageTemplate": "Non Public records must have a PUBLIC DOMAIN reference without a '%s' tag", "expression": "to_array(new)[?length(access) > `0`].references[] | [? publicDomain==`true` && contains(tags, 'PUBLIC_DOMAIN_RELEASE') && length(access) == `0`].tags[0]"}
        ]
    }
}
```

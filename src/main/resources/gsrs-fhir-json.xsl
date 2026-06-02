<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:local="urn:local"
                exclude-result-prefixes="fn xs local">

    <xsl:output method="json" encoding="UTF-8"/>

    <xsl:param name="json-input" as="xs:string" required="yes"/>

    <xsl:variable name="json-doc" select="fn:json-to-xml($json-input)"/>

    <xsl:variable name="cv-base-url" as="xs:string"
                  select="'https://gsrs.ncats.nih.gov/api/v1/vocabularies/'"/>

    <xsl:function name="local:epoch-to-iso" as="xs:string">
        <xsl:param name="epoch-ms" as="xs:string"/>
        <xsl:sequence select="fn:format-dateTime(
            xs:dateTime('1970-01-01T00:00:00Z')
                + xs:dayTimeDuration(concat('PT', xs:string(xs:integer(number($epoch-ms) div 1000)), 'S')),
            '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]Z')"/>
    </xsl:function>

    <xsl:template match="/">
        <xsl:variable name="root" select="$json-doc/fn:map"/>
        <xsl:variable name="uuid" select="string($root/fn:string[@key='uuid'])"/>
        <xsl:variable name="version" select="string($root/fn:string[@key='version'])"/>
        <xsl:variable name="substanceClass" select="string($root/fn:string[@key='substanceClass'])"/>
        <xsl:variable name="definitionType" select="string($root/fn:string[@key='definitionType'])"/>
        <xsl:variable name="definitionLevel" select="string($root/fn:string[@key='definitionLevel'])"/>
        <xsl:variable name="approvalID" select="string($root/fn:string[@key='approvalID'])"/>
        <xsl:variable name="approvedBy" select="string($root/fn:string[@key='approvedBy'])"/>
        <xsl:variable name="deprecated" select="string($root/fn:boolean[@key='deprecated'])"/>
        <xsl:variable name="created-epoch" select="string($root/fn:number[@key='created'])"/>
        <xsl:variable name="lastEdited-epoch" select="string($root/fn:number[@key='lastEdited'])"/>
        <xsl:variable name="created" select="local:epoch-to-iso($created-epoch)"/>
        <xsl:variable name="lastEdited" select="local:epoch-to-iso($lastEdited-epoch)"/>

        <xsl:variable name="transformed-xml">
            <fn:map xmlns:fn="http://www.w3.org/2005/xpath-functions">
                <fn:string key="resourceType">SubstanceDefinition</fn:string>
                <fn:string key="id"><xsl:value-of select="$uuid"/></fn:string>

                <fn:map key="meta">
                    <fn:string key="versionId"><xsl:value-of select="$version"/></fn:string>
                    <fn:string key="lastUpdated"><xsl:value-of select="$lastEdited"/></fn:string>
                </fn:map>

                <fn:array key="contained">
                    <fn:map>
                        <fn:string key="resourceType">Provenance</fn:string>
                        <fn:array key="target">
                            <fn:map>
                                <fn:string key="reference">#</fn:string>
                            </fn:map>
                        </fn:array>
                        <fn:string key="occurredDateTime"><xsl:value-of select="$created"/></fn:string>
                        <fn:string key="recorded"><xsl:value-of select="$lastEdited"/></fn:string>
                        <xsl:if test="$approvedBy">
                            <fn:array key="agent">
                                <fn:map>
                                    <fn:map key="who">
                                        <fn:string key="display"><xsl:value-of select="$approvedBy"/></fn:string>
                                    </fn:map>
                                </fn:map>
                            </fn:array>
                        </xsl:if>
                    </fn:map>
                    <xsl:if test="$root/fn:map[@key='protein']">
                        <fn:map>
                            <fn:string key="resourceType">SubstanceProtein</fn:string>
                            <fn:string key="id"><xsl:value-of select="$root/fn:map[@key='protein']/fn:string[@key='uuid']/text()"/></fn:string>
                            <xsl:variable name="prot" select="$root/fn:map[@key='protein']"/>
                            <xsl:if test="$prot/fn:string[@key='sequenceType']/text()">
                                <fn:map key="sequenceType">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'SEQUENCE_TYPE')"/></fn:string>
                                            <fn:string key="code"><xsl:value-of select="$prot/fn:string[@key='sequenceType']/text()"/></fn:string>
                                        </fn:map>
                                    </fn:array>
                                </fn:map>
                            </xsl:if>
                            <xsl:if test="$prot/fn:array[@key='subunits']/fn:map">
                                <fn:number key="numberOfSubunits"><xsl:value-of select="count($prot/fn:array[@key='subunits']/fn:map)"/></fn:number>
                            </xsl:if>
                            <xsl:if test="$prot/fn:array[@key='disulfideLinks']/fn:map">
                                <fn:array key="disulfideLinkage">
                                    <xsl:for-each select="$prot/fn:array[@key='disulfideLinks']/fn:map">
                                        <fn:string><xsl:value-of select="fn:string[@key='sitesShorthand']/text()"/></fn:string>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>
                            <xsl:if test="$prot/fn:array[@key='subunits']/fn:map">
                                <fn:array key="subunit">
                                    <xsl:for-each select="$prot/fn:array[@key='subunits']/fn:map">
                                        <fn:map>
                                            <fn:number key="subunit"><xsl:value-of select="fn:number[@key='subunitIndex']/text()"/></fn:number>
                                            <fn:string key="sequence"><xsl:value-of select="fn:string[@key='sequence']/text()"/></fn:string>
                                            <fn:number key="length"><xsl:value-of select="fn:number[@key='length']/text()"/></fn:number>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>
                        </fn:map>
                    </xsl:if>
                    <xsl:if test="$root/fn:map[@key='nucleicAcid']">
                        <fn:map>
                            <fn:string key="resourceType">SubstanceNucleicAcid</fn:string>
                            <fn:string key="id"><xsl:value-of select="$root/fn:map[@key='nucleicAcid']/fn:string[@key='uuid']/text()"/></fn:string>
                            <xsl:variable name="na" select="$root/fn:map[@key='nucleicAcid']"/>
                            <xsl:if test="$na/fn:string[@key='sequenceType']/text()">
                                <fn:map key="sequenceType">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'SEQUENCE_TYPE')"/></fn:string>
                                            <fn:string key="code"><xsl:value-of select="$na/fn:string[@key='sequenceType']/text()"/></fn:string>
                                        </fn:map>
                                    </fn:array>
                                </fn:map>
                            </xsl:if>
                            <xsl:if test="$na/fn:array[@key='subunits']/fn:map">
                                <fn:number key="numberOfSubunits"><xsl:value-of select="count($na/fn:array[@key='subunits']/fn:map)"/></fn:number>
                            </xsl:if>
                            <xsl:if test="$na/fn:string[@key='nucleicAcidType']/text()">
                                <fn:map key="oligoNucleotideType">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'NUCLEIC_ACID_TYPE')"/></fn:string>
                                            <fn:string key="code"><xsl:value-of select="$na/fn:string[@key='nucleicAcidType']/text()"/></fn:string>
                                        </fn:map>
                                    </fn:array>
                                </fn:map>
                            </xsl:if>
                            <xsl:if test="$na/fn:array[@key='subunits']/fn:map">
                                <fn:array key="subunit">
                                    <xsl:for-each select="$na/fn:array[@key='subunits']/fn:map">
                                        <fn:map>
                                            <fn:number key="subunit"><xsl:value-of select="fn:number[@key='subunitIndex']/text()"/></fn:number>
                                            <fn:string key="sequence"><xsl:value-of select="fn:string[@key='sequence']/text()"/></fn:string>
                                            <fn:number key="length"><xsl:value-of select="fn:number[@key='length']/text()"/></fn:number>
                                            <xsl:if test="position() = 1 and $na/fn:array[@key='linkages']/fn:map">
                                                <fn:array key="linkage">
                                                    <xsl:for-each select="$na/fn:array[@key='linkages']/fn:map">
                                                        <fn:map>
                                                            <fn:string key="connectivity"><xsl:value-of select="fn:string[@key='linkage']/text()"/></fn:string>
                                                            <fn:string key="residueSite"><xsl:value-of select="fn:string[@key='sitesShorthand']/text()"/></fn:string>
                                                        </fn:map>
                                                    </xsl:for-each>
                                                </fn:array>
                                            </xsl:if>
                                            <xsl:if test="position() = 1 and $na/fn:array[@key='sugars']/fn:map">
                                                <fn:array key="sugar">
                                                    <xsl:for-each select="$na/fn:array[@key='sugars']/fn:map">
                                                        <fn:map>
                                                            <fn:string key="name"><xsl:value-of select="fn:string[@key='sugar']/text()"/></fn:string>
                                                            <fn:string key="residueSite"><xsl:value-of select="fn:string[@key='sitesShorthand']/text()"/></fn:string>
                                                        </fn:map>
                                                    </xsl:for-each>
                                                </fn:array>
                                            </xsl:if>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>
                        </fn:map>
                    </xsl:if>
                </fn:array>

                <xsl:if test="$root/fn:map[@key='protein']">
                    <fn:map key="protein">
                        <fn:string key="reference"><xsl:value-of select="concat('#', $root/fn:map[@key='protein']/fn:string[@key='uuid']/text())"/></fn:string>
                    </fn:map>
                </xsl:if>

                <xsl:if test="$root/fn:map[@key='nucleicAcid']">
                    <fn:map key="nucleicAcid">
                        <fn:string key="reference"><xsl:value-of select="concat('#', $root/fn:map[@key='nucleicAcid']/fn:string[@key='uuid']/text())"/></fn:string>
                    </fn:map>
                </xsl:if>

                <xsl:if test="$approvalID">
                    <fn:array key="identifier">
                        <fn:map>
                            <fn:string key="system">https://gsrs.ncats.nih.gov/api/v1/approvalIDs</fn:string>
                            <fn:string key="value"><xsl:value-of select="$approvalID"/></fn:string>
                        </fn:map>
                    </fn:array>
                </xsl:if>

                <fn:string key="version"><xsl:value-of select="$version"/></fn:string>

                <fn:map key="status">
                    <fn:array key="coding">
                        <fn:map>
                            <fn:string key="system">http://hl7.org/fhir/publication-status</fn:string>
                            <xsl:choose>
                                <xsl:when test="$deprecated eq 'true'">
                                    <fn:string key="code">retired</fn:string>
                                </xsl:when>
                                <xsl:otherwise>
                                    <fn:string key="code">active</fn:string>
                                </xsl:otherwise>
                            </xsl:choose>
                        </fn:map>
                    </fn:array>
                </fn:map>

                <fn:array key="classification">
                    <fn:map>
                        <fn:array key="coding">
                            <fn:map>
                                <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'SUBSTANCE_CLASS')"/></fn:string>
                                <fn:string key="code"><xsl:value-of select="$substanceClass"/></fn:string>
                            </fn:map>
                        </fn:array>
                    </fn:map>
                    <xsl:if test="$definitionType">
                        <fn:map>
                            <fn:array key="coding">
                                <fn:map>
                                    <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'DEFINITION_TYPE')"/></fn:string>
                                    <fn:string key="code"><xsl:value-of select="$definitionType"/></fn:string>
                                </fn:map>
                            </fn:array>
                        </fn:map>
                    </xsl:if>
                    <xsl:if test="$definitionLevel">
                        <fn:map>
                            <fn:array key="coding">
                                <fn:map>
                                    <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'DEFINITION_LEVEL')"/></fn:string>
                                    <fn:string key="code"><xsl:value-of select="$definitionLevel"/></fn:string>
                                </fn:map>
                            </fn:array>
                        </fn:map>
                    </xsl:if>
                </fn:array>

                <fn:array key="name">
                    <xsl:for-each select="$root/fn:array[@key='names']/fn:map">
                        <fn:map>
                            <xsl:if test="fn:string[@key='uuid']">
                                <fn:string key="id"><xsl:value-of select="fn:string[@key='uuid']/text()"/></fn:string>
                            </xsl:if>
                            <fn:string key="name"><xsl:value-of select="fn:string[@key='name']/text()"/></fn:string>

                            <fn:map key="type">
                                <fn:array key="coding">
                                    <fn:map>
                                        <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'NAME_TYPE')"/></fn:string>
                                        <fn:string key="code"><xsl:value-of select="fn:string[@key='type']/text()"/></fn:string>
                                    </fn:map>
                                </fn:array>
                            </fn:map>

                            <fn:map key="status">
                                <fn:array key="coding">
                                    <fn:map>
                                        <fn:string key="system">http://hl7.org/fhir/publication-status</fn:string>
                                        <xsl:choose>
                                            <xsl:when test="fn:boolean[@key='deprecated']/text() eq 'true'">
                                                <fn:string key="code">retired</fn:string>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <fn:string key="code">active</fn:string>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </fn:map>
                                </fn:array>
                            </fn:map>

                            <fn:boolean key="preferred"><xsl:value-of select="fn:boolean[@key='displayName']/text()"/></fn:boolean>

                            <xsl:if test="fn:array[@key='languages']/fn:string">
                                <fn:array key="language">
                                    <xsl:for-each select="fn:array[@key='languages']/fn:string/text()">
                                        <fn:map>
                                            <fn:array key="coding">
                                                <fn:map>
                                                    <fn:string key="system">http://hl7.org/fhir/ValueSet/all-languages</fn:string>
                                                    <fn:string key="code"><xsl:value-of select="."/></fn:string>
                                                </fn:map>
                                             </fn:array>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>

                            <xsl:if test="fn:array[@key='domains']/fn:string">
                                <fn:array key="domain">
                                    <xsl:for-each select="fn:array[@key='domains']/fn:string/text()">
                                        <fn:map>
                                            <fn:array key="coding">
                                                <fn:map>
                                                    <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'NAME_DOMAIN')"/></fn:string>
                                                    <fn:string key="code"><xsl:value-of select="."/></fn:string>
                                                </fn:map>
                                             </fn:array>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>

                            <xsl:if test="fn:array[@key='nameJurisdiction']/fn:string">
                                <fn:array key="jurisdiction">
                                    <xsl:for-each select="fn:array[@key='nameJurisdiction']/fn:string/text()">
                                        <fn:map>
                                            <fn:array key="coding">
                                                <fn:map>
                                                    <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'JURISDICTION')"/></fn:string>
                                                    <fn:string key="code"><xsl:value-of select="."/></fn:string>
                                                </fn:map>
                                            </fn:array>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>

                            <xsl:if test="fn:array[@key='access']/fn:string">
                                <fn:array key="extension">
                                    <xsl:for-each select="fn:array[@key='access']/fn:string">
                                        <fn:map>
                                            <fn:string key="url">https://gsrs.ncats.nih.gov/fhir/extensions/access</fn:string>
                                            <fn:map key="valueCoding">
                                                <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'ACCESS_GROUP')"/></fn:string>
                                                <fn:string key="code"><xsl:value-of select="."/></fn:string>
                                            </fn:map>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>

                            <xsl:if test="fn:array[@key='nameOrgs']/fn:map">
                                <fn:array key="official">
                                    <xsl:for-each select="fn:array[@key='nameOrgs']/fn:map">
                                        <fn:map>
                                            <fn:map key="authority">
                                                <fn:array key="coding">
                                                    <fn:map>
                                                        <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'NAME_ORG')"/></fn:string>
                                                        <fn:string key="code"><xsl:value-of select="fn:string[@key='nameOrg']/text()"/></fn:string>
                                                    </fn:map>
                                                </fn:array>
                                            </fn:map>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>
                        </fn:map>
                    </xsl:for-each>
                </fn:array>

                <xsl:if test="$root/fn:array[@key='codes']/fn:map">
                    <fn:array key="code">
                        <xsl:for-each select="$root/fn:array[@key='codes']/fn:map">
                            <fn:map>
                                <xsl:if test="fn:string[@key='uuid']">
                                    <fn:string key="id"><xsl:value-of select="fn:string[@key='uuid']/text()"/></fn:string>
                                </xsl:if>
                                <fn:map key="code">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'CODE_SYSTEM/terms/', iri-to-uri(fn:string[@key='codeSystem']/text()))"/></fn:string>
                                            <fn:string key="code"><xsl:value-of select="fn:string[@key='code']/text()"/></fn:string>
                                        </fn:map>
                                    </fn:array>
                                </fn:map>
                                <xsl:if test="fn:string[@key='comments']/text()">
                                    <fn:array key="note">
                                        <fn:map>
                                            <fn:string key="text"><xsl:value-of select="fn:string[@key='comments']/text()"/></fn:string>
                                        </fn:map>
                                    </fn:array>
                                </xsl:if>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>

                <xsl:if test="$root/fn:array[@key='relationships']/fn:map">
                    <fn:array key="relationship">
                        <xsl:for-each select="$root/fn:array[@key='relationships']/fn:map">
                            <fn:map>
                                <xsl:if test="fn:string[@key='uuid']">
                                    <fn:string key="id"><xsl:value-of select="fn:string[@key='uuid']/text()"/></fn:string>
                                </xsl:if>
                                <fn:map key="substanceDefinitionReference">
                                    <fn:string key="reference">
                                        <xsl:value-of select="concat('SubstanceDefinition/', fn:map[@key='relatedSubstance']/fn:string[@key='refuuid']/text())"/>
                                    </fn:string>
                                </fn:map>
                                <fn:map key="type">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'RELATIONSHIP_TYPE')"/></fn:string>
                                            <fn:string key="code"><xsl:value-of select="fn:string[@key='type']/text()"/></fn:string>
                                        </fn:map>
                                    </fn:array>
                                </fn:map>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>

                <xsl:if test="$root/fn:array[@key='notes']/fn:map">
                    <fn:array key="note">
                        <xsl:for-each select="$root/fn:array[@key='notes']/fn:map">
                            <fn:map>
                                <fn:string key="text"><xsl:value-of select="fn:string[@key='note']/text()"/></fn:string>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>

                <xsl:if test="$root/fn:array[@key='properties']/fn:map">
                    <fn:array key="property">
                        <xsl:for-each select="$root/fn:array[@key='properties']/fn:map">
                            <fn:map>
                                <fn:map key="type">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'PROPERTY_NAME')"/></fn:string>
                                            <fn:string key="code"><xsl:value-of select="fn:string[@key='name']/text()"/></fn:string>
                                        </fn:map>
                                    </fn:array>
                                </fn:map>
                                <xsl:if test="fn:map[@key='value']">
                                    <xsl:variable name="v" select="fn:map[@key='value']"/>
                                    <xsl:variable name="non-num" select="string($v/fn:string[@key='nonNumericValue'])"/>
                                    <xsl:variable name="avg" select="string($v/fn:number[@key='average'])"/>
                                    <xsl:variable name="low" select="string($v/fn:number[@key='low'])"/>
                                    <xsl:variable name="high" select="string($v/fn:number[@key='high'])"/>
                                    <xsl:variable name="units" select="string($v/fn:string[@key='units'])"/>
                                    <xsl:choose>
                                        <xsl:when test="$non-num">
                                            <fn:map key="valueCodeableConcept">
                                                <fn:string key="text"><xsl:value-of select="$non-num"/></fn:string>
                                            </fn:map>
                                        </xsl:when>
                                        <xsl:when test="$avg != '' or $low != '' or $high != ''">
                                            <fn:map key="valueQuantity">
                                                <xsl:if test="$avg != ''">
                                                    <fn:number key="value"><xsl:value-of select="$avg"/></fn:number>
                                                </xsl:if>
                                                <xsl:if test="$units">
                                                    <fn:string key="unit"><xsl:value-of select="$units"/></fn:string>
                                                </xsl:if>
                                            </fn:map>
                                        </xsl:when>
                                    </xsl:choose>
                                </xsl:if>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>

                <xsl:if test="$root/fn:map[@key='structure']/fn:number[@key='mwt']">
                    <fn:array key="molecularWeight">
                        <fn:map>
                            <fn:map key="amount">
                                <fn:number key="value"><xsl:value-of select="round(number($root/fn:map[@key='structure']/fn:number[@key='mwt']/text()) * 100) div 100"/></fn:number>
                            </fn:map>
                        </fn:map>
                    </fn:array>
                </xsl:if>

                <xsl:if test="$root/fn:map[@key='structure']">
                    <fn:map key="structure">
                        <xsl:variable name="s" select="$root/fn:map[@key='structure']"/>
                        <xsl:variable name="formula" select="string($s/fn:string[@key='formula'])"/>
                        <xsl:variable name="mwt" select="string($s/fn:number[@key='mwt'])"/>
                        <xsl:variable name="molfile" select="string($s/fn:string[@key='molfile'])"/>
                        <xsl:variable name="smiles" select="string($s/fn:string[@key='smiles'])"/>
                        <xsl:variable name="inchi" select="string($s/fn:string[@key='_inchi'])"/>
                        <xsl:variable name="inchi-key" select="string($s/fn:string[@key='_inchiKey'])"/>
                        <xsl:variable name="stereo" select="string($s/fn:string[@key='stereochemistry'])"/>
                        <xsl:variable name="opt" select="string($s/fn:string[@key='opticalActivity'])"/>
                        <xsl:variable name="repr-format-system" select="'http://hl7.org/fhir/ValueSet/substance-representation-format'"/>

                        <xsl:if test="$formula">
                            <fn:string key="molecularFormula"><xsl:value-of select="$formula"/></fn:string>
                        </xsl:if>
                        <xsl:if test="$mwt != ''">
                            <fn:map key="molecularWeight">
                                <fn:map key="amount">
                                    <fn:number key="value"><xsl:value-of select="round(number($mwt) * 100) div 100"/></fn:number>
                                </fn:map>
                            </fn:map>
                        </xsl:if>
                        <xsl:if test="$molfile or $smiles or $inchi or $inchi-key">
                            <fn:array key="representation">
                                <xsl:if test="$molfile">
                                    <fn:map>
                                        <fn:map key="format">
                                            <fn:array key="coding">
                                                <fn:map>
                                                    <fn:string key="system"><xsl:value-of select="$repr-format-system"/></fn:string>
                                                    <fn:string key="code">MOLFILE</fn:string>
                                                </fn:map>
                                            </fn:array>
                                        </fn:map>
                                        <fn:string key="representation"><xsl:value-of select="$molfile"/></fn:string>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$smiles">
                                    <fn:map>
                                        <fn:map key="format">
                                            <fn:array key="coding">
                                                <fn:map>
                                                    <fn:string key="system"><xsl:value-of select="$repr-format-system"/></fn:string>
                                                    <fn:string key="code">SMILES</fn:string>
                                                </fn:map>
                                            </fn:array>
                                        </fn:map>
                                        <fn:string key="representation"><xsl:value-of select="$smiles"/></fn:string>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$inchi">
                                    <fn:map>
                                        <fn:map key="format">
                                            <fn:array key="coding">
                                                <fn:map>
                                                    <fn:string key="system"><xsl:value-of select="$repr-format-system"/></fn:string>
                                                    <fn:string key="code">InChI</fn:string>
                                                </fn:map>
                                            </fn:array>
                                        </fn:map>
                                        <fn:string key="representation"><xsl:value-of select="$inchi"/></fn:string>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$inchi-key">
                                    <fn:map>
                                        <fn:map key="format">
                                            <fn:array key="coding">
                                                <fn:map>
                                                    <fn:string key="system"><xsl:value-of select="$repr-format-system"/></fn:string>
                                                    <fn:string key="code">InChI-Key</fn:string>
                                                </fn:map>
                                            </fn:array>
                                        </fn:map>
                                        <fn:string key="representation"><xsl:value-of select="$inchi-key"/></fn:string>
                                    </fn:map>
                                </xsl:if>
                            </fn:array>
                        </xsl:if>
                        <xsl:if test="$stereo">
                            <fn:map key="stereochemistry">
                                <fn:array key="coding">
                                    <fn:map>
                                        <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'STEREOCHEMISTRY_TYPE')"/></fn:string>
                                        <fn:string key="code"><xsl:value-of select="$stereo"/></fn:string>
                                    </fn:map>
                                </fn:array>
                            </fn:map>
                        </xsl:if>
                        <xsl:if test="$opt">
                            <fn:map key="opticalActivity">
                                <fn:array key="coding">
                                    <fn:map>
                                        <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'OPTICAL_ACTIVITY')"/></fn:string>
                                        <fn:string key="code"><xsl:value-of select="$opt"/></fn:string>
                                    </fn:map>
                                </fn:array>
                            </fn:map>
                        </xsl:if>
                    </fn:map>
                </xsl:if>

                <xsl:if test="$root/fn:array[@key='moieties']/fn:map">
                    <fn:array key="moiety">
                        <xsl:for-each select="$root/fn:array[@key='moieties']/fn:map">
                            <xsl:variable name="m" select="."/>
                            <fn:map>
                                <xsl:variable name="role" select="string($m/fn:string[@key='role'])"/>
                                <xsl:variable name="formula" select="string($m/fn:string[@key='formula'])"/>
                                <xsl:variable name="stereo" select="string($m/fn:string[@key='stereochemistry'])"/>
                                <xsl:variable name="opt" select="string($m/fn:string[@key='opticalActivity'])"/>
                                <xsl:variable name="count-avg" select="string($m/fn:map[@key='countAmount']/fn:number[@key='average'])"/>
                                <xsl:variable name="count-units" select="string($m/fn:map[@key='countAmount']/fn:string[@key='units'])"/>
                                <xsl:variable name="count-type" select="string($m/fn:map[@key='countAmount']/fn:string[@key='type'])"/>

                                <xsl:if test="$role">
                                    <fn:map key="role">
                                        <fn:array key="coding">
                                            <fn:map>
                                                <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'ROLE')"/></fn:string>
                                                <fn:string key="code"><xsl:value-of select="$role"/></fn:string>
                                            </fn:map>
                                        </fn:array>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$m/fn:string[@key='uuid']/text()">
                                    <fn:map key="identifier">
                                        <fn:string key="system">urn:ietf:rfc:3986</fn:string>
                                        <fn:string key="value"><xsl:value-of select="concat('urn:uuid:', $m/fn:string[@key='uuid']/text())"/></fn:string>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$formula">
                                    <fn:string key="name"><xsl:value-of select="$formula"/></fn:string>
                                </xsl:if>
                                <xsl:if test="$stereo">
                                    <fn:map key="stereochemistry">
                                        <fn:array key="coding">
                                            <fn:map>
                                                <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'STEREOCHEMISTRY_TYPE')"/></fn:string>
                                                <fn:string key="code"><xsl:value-of select="$stereo"/></fn:string>
                                            </fn:map>
                                        </fn:array>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$opt">
                                    <fn:map key="opticalActivity">
                                        <fn:array key="coding">
                                            <fn:map>
                                                <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'OPTICAL_ACTIVITY')"/></fn:string>
                                                <fn:string key="code"><xsl:value-of select="$opt"/></fn:string>
                                            </fn:map>
                                        </fn:array>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$formula">
                                    <fn:string key="molecularFormula"><xsl:value-of select="$formula"/></fn:string>
                                </xsl:if>
                                <xsl:if test="$count-avg != ''">
                                    <fn:map key="amountQuantity">
                                        <fn:number key="value"><xsl:value-of select="$count-avg"/></fn:number>
                                        <xsl:if test="$count-units">
                                            <fn:string key="unit"><xsl:value-of select="$count-units"/></fn:string>
                                        </xsl:if>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$count-type">
                                    <fn:map key="measurementType">
                                        <fn:array key="coding">
                                            <fn:map>
                                                <fn:string key="system"><xsl:value-of select="concat($cv-base-url, 'MONOMER_AMOUNT_TYPE')"/></fn:string>
                                                <fn:string key="code"><xsl:value-of select="$count-type"/></fn:string>
                                            </fn:map>
                                        </fn:array>
                                    </fn:map>
                                </xsl:if>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:variable>

        <xsl:value-of select="fn:xml-to-json($transformed-xml)
            => fn:parse-json()
            => fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
    </xsl:template>

</xsl:stylesheet>

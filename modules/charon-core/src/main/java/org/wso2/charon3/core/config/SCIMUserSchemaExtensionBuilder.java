/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.charon3.core.config;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.InternalErrorException;
import org.wso2.charon3.core.schema.AttributeSchema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.wso2.charon3.core.schema.SCIMConstants.ENTERPRISE_USER_SCHEMA_URI;

/**
 * This class is to build the extension user schema though the config file.
 */
public class SCIMUserSchemaExtensionBuilder extends ExtensionBuilder {

    private static SCIMUserSchemaExtensionBuilder configReader = new SCIMUserSchemaExtensionBuilder();
    // configuration map
    private static Map<String, ExtensionAttributeSchemaConfig> extensionConfig = new HashMap<>();
    // Extension root attribute name.
    String extensionRootAttributeName = null;
    String extensionRootAttributeURI;
    // built schema map
    private static Map<String, AttributeSchema> attributeSchemas = new HashMap<>();
    // extension root attribute schema
    private AttributeSchema extensionSchema = null;

    public static SCIMUserSchemaExtensionBuilder getInstance() {
        return configReader;
    }

    /**
     * Constructor to initialize the SCIMUserSchemaExtensionBuilder.
     */
    public SCIMUserSchemaExtensionBuilder() {

        this.extensionRootAttributeURI = ENTERPRISE_USER_SCHEMA_URI;
    }

    public AttributeSchema getExtensionSchema() {
        return extensionSchema;
    }

    /*
     * Logic goes here
     * @throws CharonException
     */
    public void buildUserSchemaExtension(String configFilePath) throws CharonException, InternalErrorException {
        File provisioningConfig = new File(configFilePath);
        try (InputStream configFilePathInputStream = new FileInputStream(provisioningConfig)) {
            buildUserSchemaExtension(configFilePathInputStream);
        } catch (FileNotFoundException e) {
            throw new CharonException(SCIMConfigConstants.SCIM_SCHEMA_EXTENSION_CONFIG + " file not found!",
                    e);
        } catch (JSONException e) {
            throw new CharonException("Error while parsing " +
                    SCIMConfigConstants.SCIM_SCHEMA_EXTENSION_CONFIG + " file!", e);
        } catch (IOException e) {
            throw new CharonException("Error while closing " +
                    SCIMConfigConstants.SCIM_SCHEMA_EXTENSION_CONFIG + " file!", e);
        }
    }

    public void buildUserSchemaExtension(InputStream inputStream) throws CharonException, InternalErrorException {

        readConfiguration(inputStream);
        for (Map.Entry<String, ExtensionAttributeSchemaConfig> attributeSchemaConfig : extensionConfig.entrySet()) {
            // If there are no children it's a simple attribute, build it.
            if (!attributeSchemaConfig.getValue().hasChildren()) {
                buildSimpleAttributeSchema(attributeSchemaConfig.getValue(), attributeSchemas);
            } else {
                // Need to build child schemas first.
                buildComplexAttributeSchema(attributeSchemaConfig.getValue(), attributeSchemas, extensionConfig);
            }
        }

        extensionSchema = attributeSchemas.get(extensionRootAttributeURI);
    }

    public void readConfiguration(InputStream inputStream) throws CharonException {

        Scanner scanner = new Scanner(inputStream, "utf-8").useDelimiter("\\A");
        String jsonString = scanner.hasNext() ? scanner.next() : "";

        JSONArray attributeConfigArray = new JSONArray(jsonString);

        for (int index = 0; index < attributeConfigArray.length(); ++index) {
            JSONObject rawAttributeConfig = attributeConfigArray.getJSONObject(index);
            ExtensionAttributeSchemaConfig schemaAttributeConfig =
                    new ExtensionAttributeSchemaConfig(rawAttributeConfig);
            if (schemaAttributeConfig.getURI().startsWith(extensionRootAttributeURI)) {
                extensionConfig.put(schemaAttributeConfig.getURI(), schemaAttributeConfig);
            }

            if (extensionRootAttributeURI.equals(schemaAttributeConfig.getURI())) {
                extensionRootAttributeName = schemaAttributeConfig.getName();
            }
        }
    }


    @Override
    public String getURI() {

        return extensionRootAttributeURI;
    }

    @Override
    protected boolean isRootConfig(ExtensionAttributeSchemaConfig config) {

        return StringUtils.isNotBlank(extensionRootAttributeName) &&
                extensionRootAttributeName.equals(config.getName());
    }
}


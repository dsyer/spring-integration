/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jdbc.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.JdbcOutboundGateway;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * 
 * @since 2.0
 *
 */
public class JdbcOutboundGatewayParser extends AbstractConsumerEndpointParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		String dataSourceRef = element.getAttribute("data-source");
		String jdbcOperationsRef = element.getAttribute("jdbc-operations");
		boolean refToDataSourceSet = StringUtils.hasText(dataSourceRef);
		boolean refToJdbcOperationsSet = StringUtils.hasText(jdbcOperationsRef);
		if ((refToDataSourceSet && refToJdbcOperationsSet) || (!refToDataSourceSet && !refToJdbcOperationsSet)) {
			parserContext.getReaderContext().error(
					"Exactly one of the attributes data-source or "
							+ "simple-jdbc-operations should be set for the JDBC outbound-gateway", element);
		}
		String selectQuery = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "query",
				parserContext);
		if (!StringUtils.hasText(selectQuery)) {
			selectQuery = null;
		}
		String updateQuery = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "update",
				parserContext);
		if (!StringUtils.hasText(updateQuery)) {
			parserContext.getReaderContext().error("The update attribute is required", element);
			return null;
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(JdbcOutboundGateway.class);
		if (refToDataSourceSet) {
			builder.addConstructorArgReference(dataSourceRef);
		}
		else {
			builder.addConstructorArgReference(jdbcOperationsRef);
		}

		builder.getRawBeanDefinition().getConstructorArgumentValues().addIndexedArgumentValue(1, updateQuery);
		builder.getRawBeanDefinition().getConstructorArgumentValues().addIndexedArgumentValue(2, selectQuery);

		IntegrationNamespaceUtils
				.setReferenceIfAttributeDefined(builder, element, "reply-sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				"request-sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "row-mapper");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-rows-per-poll");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "keys-generated");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		
		String replyChannel = element.getAttribute("reply-channel");
		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyReference("outputChannel", replyChannel);
		}

		return builder;

	}

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}
}

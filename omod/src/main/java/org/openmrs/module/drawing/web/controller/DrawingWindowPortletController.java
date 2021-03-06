/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.drawing.web.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.drawing.DrawingUtil;
import org.openmrs.web.controller.PortletController;

/**
 *
 */
public class DrawingWindowPortletController extends PortletController {
	
	private static final Log log = LogFactory.getLog(DrawingWindowPortletController.class);
	
	@Override
	protected void populateModel(HttpServletRequest request, Map<String, Object> model) {
		if (log.isDebugEnabled())
			log.debug("In DrawingWindowPortletController...");
		
		model.put("encodedTemplateNames", DrawingUtil.getAllTemplateNames());
		
	}
}

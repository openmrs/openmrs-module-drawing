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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.drawing.AnnotatedImage;
import org.openmrs.module.drawing.DrawingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The main controller.
 */
@Controller
public class DrawingManageController {
	
	protected final Log log = LogFactory.getLog(getClass());
	

	
	@RequestMapping(value = "/module/drawing/manage.form", method = RequestMethod.GET)

	public void manage(ModelMap model, HttpServletRequest request) {
		
		if (StringUtils.isNotBlank(request.getParameter("obsId"))) {
			int obsId = Integer.parseInt(request.getParameter("obsId"));
			Obs obs = Context.getObsService().getObs(obsId);
			if (obs == null || !obs.getConcept().isComplex()) {
				log.error("obs is not complex ");
			} else {
				
				AnnotatedImage ai = new AnnotatedImage(new String((byte[]) obs.getComplexData().getData()));
				
				//TODO for TFS-145140, pull most recent annotation text from db here
				
				String svgMarkup = DrawingUtil.documentToString(ai.getImageDocument());
				
				svgMarkup = StringEscapeUtils.escapeHtml(svgMarkup);
				
				model.addAttribute("svgDOM", svgMarkup);
				
				//TODO Does this need to provide a way to access annotations separately?
				//model.addAttribute("annotations", ai.getAnnotations());
				model.addAttribute("obsId", obs.getId());
				
			}
		}
	}
	
}

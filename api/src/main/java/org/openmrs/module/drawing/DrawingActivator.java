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
package org.openmrs.module.drawing;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.drawing.handlers.DrawingTagHandler;
import org.openmrs.module.drawing.DrawingConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class DrawingActivator implements ModuleActivator {
	
	protected Log log = LogFactory.getLog(getClass());
	
	/**
	 * @see ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("Refreshing Drawing Module");
		
	}
	
	/**
	 * @see ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		log.info("Drawing Module refreshed");
		
	}
	
	/**
	 * @see ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("Starting Drawing Module");
		
	}
	
	/**
	 * @see ModuleActivator#started()
	 */
	public void started() {
		log.info("Drawing Module started");
		
		boolean hfesStarted = ModuleFactory.isModuleStarted("htmlformentry");
		
		ConceptService conceptService = Context.getConceptService();
		
		{
			final String name = "SVG TEXT ANNOTATION";
			final String desc = "Concept for 'SVG attachment' text annotation (used by drawing module)";
			final String uuid = DrawingConstants.CONCEPT_SVG_TEXT_UUID;
			
			DrawingConstants.textAnnConcept = conceptService.getConceptByUuid(uuid);
			
			//if this concept does not yet exist in the db
			if (null == DrawingConstants.textAnnConcept) {
				
				//create it
				Concept textConcept = new Concept();
				textConcept.setUuid(uuid);
				ConceptName conceptName = new ConceptName(name, Locale.ENGLISH);
				textConcept.setFullySpecifiedName(conceptName);
				textConcept.setPreferredName(conceptName);
				textConcept.setConceptClass(conceptService.getConceptClassByUuid(ConceptClass.QUESTION_UUID));
				textConcept.setDatatype(conceptService.getConceptDatatypeByUuid(ConceptDatatype.TEXT_UUID));
				textConcept.addDescription(new ConceptDescription(desc, Locale.ENGLISH));
				
				//store it
				DrawingConstants.textAnnConcept = conceptService.saveConcept(textConcept);
			}
		}
		
		{
			final String name = "SVG OBS GROUP";
			final String desc = "Concept for grouping 'SVG attachment' complex obs and text annotations (used by drawing module)";
			final String uuid = DrawingConstants.CONCEPT_SVG_GROUP_UUID;
			
			DrawingConstants.svgGroupConcept = conceptService.getConceptByUuid(uuid);
			
			//if this concept does not yet exist in the db
			if (null == DrawingConstants.svgGroupConcept) {
				
				//create it
				Concept groupConcept = new Concept();
				groupConcept.setUuid(uuid);
				ConceptName conceptName = new ConceptName(name, Locale.ENGLISH);
				groupConcept.setFullySpecifiedName(conceptName);
				groupConcept.setPreferredName(conceptName);
				groupConcept.setConceptClass(conceptService.getConceptClassByUuid(ConceptClass.CONVSET_UUID));
				groupConcept.setDatatype(conceptService.getConceptDatatypeByUuid(ConceptDatatype.N_A_UUID));
				groupConcept.addDescription(new ConceptDescription(desc, Locale.ENGLISH));
				
				//store it
				DrawingConstants.svgGroupConcept = conceptService.saveConcept(groupConcept);
			}
		}
		
		if (hfesStarted) {
			try {
				
				//register CONCEPT_SVG_TEXT_GUID
				
				HtmlFormEntryService hfes = Context.getService(HtmlFormEntryService.class);
				
				hfes.addHandler(DrawingConstants.DRAWING_TAG, new DrawingTagHandler());
				
				log.info("drawing : drawing tag registered");
			}
			catch (Exception ex) {
				log.error("failed to register drawing tag in drawing", ex);
				
			}
			
		}
		
	}
	
	/**
	 * @see ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("Stopping Drawing Module");
	}
	
	/**
	 * @see ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info("Drawing Module stopped");
	}
	
}

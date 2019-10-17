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

/**
 *
 */
public class DrawingConstants {
	
	public static final String DRAWING_TAG = "drawing";
	
	public static final String DRAWINGDIRECTORY = "DrawingTemplates";
	
	public static final String BASE_COMPLEX_OBS_FILENAME = "drawingObs.svg";
	
	public static final String[] ACCEPTDEXTENSIONS = { "JPEG", "JPG", "PNG" };
	
	private static final String VALID_USAGE_EXAMPLE_MSG = ", e.g. <drawing conceptId=\"<validComplexConceptIdentifier>\" id=\"<domInstanceUniqueId>\" displayMode=\"<[Annotation, Signature]>\"/>";
	
	public static final String CONCEPT_ID_MISSING_MSG = "<drawing> tag must have a Concept Id/SAME-AS/UUID attribute defined"
	        + VALID_USAGE_EXAMPLE_MSG;
	
	public static final String CONCEPT_SIMPLE_MSG = "Concept Id/SAME-AS/UUID must be for a complex concept";
	
	public static final String CONCEPT_NOT_FOUND_MSG = "Cannot find concept for value %s"
	        + " in conceptId attribute value. Parameters: %s";
	
	public static final String MARKUP_ID_MISSING_MSG = "<drawing> tag must have an id attribute" + VALID_USAGE_EXAMPLE_MSG;
	
	public static final String DISPLAY_MODE_UNK_MSG = "Unknown display mode: '%s'" + " Known modes are %s";
	
	public static final String DIM_MISSING_UNITS_MSG = "<drawing> %s attribute must have units, e.g. %s='%spx'";
	
	//this is actually defined within the pom.xml as <targetPath>web/module</targetPath>
	//and the existing resources directory path under webapps
	public static final String RELATIVE_HTML_PATH = "web/module/resources/SVG.html";
	
	//uses SVG.html in resources,
	//the one centralized resource for all places these controls are used in the module
	//instead of duplicating markup (and using stringbuilder so excessively :/ )
	
	//init and reuse this path, this should be the path specified
	//in the pom.xml for drawing-omod resources 
	//( e.g. web/module ) and then the directory tree in source under the root
	//( e.g. web/module/resources/SVG.html )
	public static final String EDITOR_HTML_PATH = DrawingUtil.getServerResourcesPath(DrawingConstants.RELATIVE_HTML_PATH);
	
}

package org.openmrs.module.drawing.web.controller;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.drawing.DrawingConstants;
import org.openmrs.module.drawing.DrawingUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.drawing.AnnotatedImage;
import org.openmrs.module.drawing.DrawingUtil.ErrorStatus;
import org.openmrs.obs.ComplexData;
import org.openmrs.validator.ObsValidator;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DrawingWindowController {
	
	private Log log = LogFactory.getLog(DrawingWindowController.class);
	
	@RequestMapping(value = "/module/drawing/saveDrawing", method = RequestMethod.POST)
	public String saveDrawing(@RequestParam(value = "svgDOM", required = true) String svgDOM,
	        @RequestParam(value = "patientId", required = true) Patient patient,
	        @RequestParam(value = "conceptId", required = true) Concept concept,
	        @RequestParam(value = "encounterId", required = false) Encounter encounter,
	        @RequestParam(value = "date", required = true) String dateString,
	        @RequestParam(value = "redirectUrl", required = true) String redirectUrl, HttpServletRequest request) {
		
		if (StringUtils.isBlank(redirectUrl))
			redirectUrl = "/patientDashboard.form?patientId=" + patient.getPatientId();
		
		try {
			Date date = StringUtils.isBlank(dateString) ? new Date() : Context.getDateFormat().parse(dateString);
			Obs o = new Obs(patient, concept, date, null);
			o.setEncounter(encounter);
			AnnotatedImage ai = new AnnotatedImage(svgDOM);
			//ai.setAnnotations(DrawingUtil.getAnnotations(request, ""));
			
			//saveObs expects byte array
			byte[] svgByteArray = DrawingUtil.documentToString(ai.getImageDocument()).getBytes();
			
			o.setComplexData(new ComplexData(DrawingConstants.BASE_COMPLEX_OBS_FILENAME, svgByteArray));
			Errors obsErrors = new BindException(o, "obs");
			ValidationUtils.invokeValidator(new ObsValidator(), o, obsErrors);
			if (!obsErrors.hasErrors()) {
				Context.getObsService().saveObs(o, "saving obs");
				request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "drawing.saved");
			} else {
				obsErrors.getFieldErrors();
				String s = "";
				for (FieldError e : obsErrors.getFieldErrors()) {
					s = s + e.getField() + " cannot be " + e.getRejectedValue() + "</br>";
					request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, s);
				}
				
			}
		}
		catch (Exception e) {
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ARGS, "drawing.save.error");
		}
		return "redirect:" + redirectUrl;
		
	}
	
	@RequestMapping(value = "/module/drawing/updateDrawing", method = RequestMethod.POST)
	public String updateDrawing(@RequestParam(value = "svgDOM", required = true) String svgDOM,
	        @RequestParam(value = "obsId", required = true) String obsId, HttpServletRequest request) throws IOException {
		
		Obs existingObs = Context.getObsService().getObs(Integer.parseInt(obsId.trim()));
		
		//according to comments on ObsService saveObs()
		//https://github.com/openmrs/openmrs-core/blob/4a0feb8da351088f25fdc4e6d324a1f277aa3410/api/src/main/java/org/openmrs/api/ObsService.java#L100-L107
		//a lot of this process should be handled by it, 
		//but according to Obs.java it should be done manually
		//https://github.com/openmrs/openmrs-core/blob/4a0feb8da351088f25fdc4e6d324a1f277aa3410/api/src/main/java/org/openmrs/Obs.java#L57-L60
		//since the handler is called for complex obs, maybe any common update logic
		//should be moved there, but "updates" vs. new saves would
		//then need to be detected
		
		//copy ctor and setPreviousVersion
		Obs newDerivedObs = Obs.newInstance(existingObs);
		newDerivedObs.setPreviousVersion(existingObs);
		
		//reference obs to redirect to after the update process is completed
		//default to case where no new obs is created
		Obs obsToShow = existingObs;
		
		if (existingObs == null) {
			log.error("obs cannot be null");
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "drawing.save.error");
		}
		
		AnnotatedImage ai = new AnnotatedImage(new String((byte[]) existingObs.getComplexData().getData()));
		
		ai.setImageDocument(svgDOM);
		ErrorStatus result = ai.getParsingError();
		
		if (result == ErrorStatus.NONE) {
			
			//using getTitle() during an update will return the title with the uuid suffix
			//leading to a filename like "base_olduuid_newuuid.svg"
			//providing just the base filename will create the expected "base_uuid.svg"
			//the previous file is deleted?
			newDerivedObs.setComplexData(new ComplexData(DrawingConstants.BASE_COMPLEX_OBS_FILENAME, ai));
			
			Context.getObsService().voidObs(existingObs, "creating update obs from existing obs");
			Context.getObsService().saveObs(newDerivedObs, "creating update obs from existing obs");
			
			obsToShow = newDerivedObs;
		} else {
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, result.toString());
			//request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ARGS, "drawing.save.error");	
		}
		
		return "redirect:/module/drawing/manage.form?obsId=" + obsToShow.getId();
	}
	
}

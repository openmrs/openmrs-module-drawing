package org.openmrs.module.drawing.handlers;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.openmrs.module.drawing.elements.DrawingSubmissionElement;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler;
import org.xml.sax.SAXException;

public class DrawingTagHandler extends SubstitutionTagHandler {
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	        Map<String, String> parameters) throws BadFormDesignException {
		
		DrawingSubmissionElement element = null;
		
		try {
			element = new DrawingSubmissionElement(session.getContext(), parameters);
		}
		catch (ParserConfigurationException | SAXException | IOException e) {
			BadFormDesignException formEx = new BadFormDesignException(e.getMessage());
			formEx.addSuppressed(e);
			
			throw formEx;
		}
		
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());
	}
	
}

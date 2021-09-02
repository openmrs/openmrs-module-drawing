package org.openmrs.module.drawing.elements;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.drawing.AnnotatedImage;
import org.openmrs.module.drawing.DrawingConstants;
import org.openmrs.module.drawing.DrawingUtil;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionActions;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
//import org.openmrs.module.htmlformentry.schema.ObsGroup;
//import org.openmrs.module.htmlformentry.action.ObsGroupAction;
import org.openmrs.obs.ComplexData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DrawingSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	private static final Log log = LogFactory.getLog(DrawingSubmissionElement.class);
	
	private String htmlId;
	
	private Concept questionConcept;
	
	private Obs parentObs;
	
	private enum DisplayMode {
		Invalid,
		Annotation,
		Signature
	};
	
	private DisplayMode instancePurpose = DisplayMode.Invalid;
	
	private String base64preload;
	
	private List<String> excludedButtonIds;
	
	private boolean required;
	
	private String fixedWidth;
	
	private String fixedHeight;
	
	private String defaultViewportWidth;
	
	private String defaultViewportHeight;
	
	private String defaultTool;
	
	private Document svgWidgetTemplate;
	
	public DrawingSubmissionElement(FormEntryContext context, Map<String, String> parameters)
	        throws IOException, ParserConfigurationException, SAXException {
		
		String questionConceptId = parameters.get("conceptId");
		
		//while this is not strictly necessary, it is good practice
		//it should be worth continuing to enforce this requirement in the long run
		htmlId = parameters.get("id");
		
		if (StringUtils.isBlank(questionConceptId))
			throw new RuntimeException(DrawingConstants.CONCEPT_ID_MISSING_MSG);
		else if (StringUtils.isBlank(htmlId))
			throw new RuntimeException(DrawingConstants.MARKUP_ID_MISSING_MSG);
		
		questionConcept = HtmlFormEntryUtil.getConcept(questionConceptId);
		
		if (questionConcept == null) {
			String exceptionStr = String.format(DrawingConstants.CONCEPT_NOT_FOUND_MSG, questionConceptId, parameters);
			
			throw new IllegalArgumentException(exceptionStr);
		}
		
		if (!questionConcept.isComplex()) {
			throw new IllegalArgumentException(DrawingConstants.CONCEPT_SIMPLE_MSG);
		}
		
		String requiredParam = parameters.get("required");
		
		//Java should have already defaulted the value to false/0
		required = false;
		
		if (requiredParam != null) {
			required = requiredParam.equals("true");
		}
		
		fixedWidth = parameters.get("width");
		fixedHeight = parameters.get("height");
		
		if (fixedHeight != null) {
			boolean lastHeightCharIsNumber = StringUtils.isNumeric(fixedHeight.substring(fixedHeight.length() - 1));
			
			if (lastHeightCharIsNumber) {
				throw new IllegalArgumentException(
				        String.format(DrawingConstants.DIM_MISSING_UNITS_MSG, "height", "height", fixedHeight));
			}
		}
		
		if (fixedWidth != null) {
			boolean lastWidthCharIsNumber = StringUtils.isNumeric(fixedWidth.substring(fixedWidth.length() - 1));
			
			if (lastWidthCharIsNumber) {
				throw new IllegalArgumentException(
				        String.format(DrawingConstants.DIM_MISSING_UNITS_MSG, "width", "width", fixedWidth));
			}
		}
		
		//Limit features exposed to the user given the intended use of this instance
		String usedFor = parameters.get("displayMode");
		
		//if the string doesnt exist default to blank string
		if (usedFor == null) {
			usedFor = "";
		}
		
		for (DisplayMode mode : DisplayMode.values()) {
			if (usedFor.equals(mode.toString().toLowerCase())) {
				instancePurpose = mode;
			}
		}
		
		if (instancePurpose == DisplayMode.Invalid) {
			String modes = Arrays.asList(DisplayMode.values()).stream().map(new Function<DisplayMode, String>() {
				
				@Override
				public String apply(DisplayMode arg0) {
					return arg0.toString();
				}
			}).collect(Collectors.joining(","));
			//"Unknown display mode: " + usedFor + " . Known modes are "+ modes
			throw new IllegalArgumentException(String.format(DrawingConstants.DISPLAY_MODE_UNK_MSG, usedFor, modes));
		}
		
		//Preload a "template" image into the SVG
		String preloadImage = parameters.get("preloadResImage");
		
		defaultViewportWidth = "500px";
		defaultViewportHeight = "250px";
		
		if (preloadImage != null) {
			
			//get the classes path for modules
			String resourcePath = DrawingUtil.getServerResourcesPath(preloadImage);
			
			//in the view resources for modules
			//String modulesPath = classesPath + "../view/module/";
			
			//File preloadImageFile = new File("/WEB-INF/view/module/"+preloadImage);
			//load the path for the specific file from a given module
			File preloadImageFile = new File(resourcePath);
			
			//allow the IOException to be thrown, getServerResourcesPath should
			//throw if no resource was found, but if something else is going wrong
			//this could indicate it to a module developer, even if it's not obvious to 
			//the form designer(s)
			
			String extension = DrawingUtil.getExtension(preloadImageFile.getName());
			BufferedImage bi = ImageIO.read(preloadImageFile);
			base64preload = DrawingUtil.imageToBase64(bi, extension);
			
			defaultViewportWidth = Integer.toString(bi.getWidth()) + "px";
			defaultViewportHeight = Integer.toString(bi.getHeight()) + "px";
		}
		
		String excludeButtons = parameters.get("excludeButtons");
		if (excludeButtons != null) {
			String[] ids = excludeButtons.split(",");
			
			for (int i = 0; i < ids.length; i++) {
				ids[i] = ids[i].trim();
			}
			
			excludedButtonIds = Arrays.asList(ids);
		}
		
		Map<Concept, List<Obs>> prevObs = context.getExistingObs();

		if (prevObs != null && prevObs.size() > 0) {

			List<Obs> obs = prevObs.get(questionConcept);

			if (obs != null) {
				parentObs = obs.get(0);

				//make sure the instance is loaded with complexData, according
				//to getComplexData() comments it won't be guaranteed unless
				//we load it this way

				//this also marks the obs as dirty, so it must always be
				//saved via modifyObs when in EDIT mode, even if it wasn't changed,
				//otherwise the dirty obs flag causes saveExistingObs to be called which voids
				//(and thereby deletes the complex obs file)
				//by not triggering the set of the dirty flag, saveObsNotDirty
				//will be called in ObsServiceImpl.saveObs() instead
				parentObs = Context.getObsService().getObs(parentObs.getId());
			}
		}
		
		//TODO move to static (or does it require autowiring?)
		
		//preloaded markup among all instances w/ a deep clone?
		//(which over head is greater? clone or read from disk?)
		svgWidgetTemplate = DrawingUtil.loadEditorMarkup(DrawingConstants.EDITOR_HTML_PATH);
		
		//opening the form for view/edit should not remove the existing observation
		//TODO evaluate supporting the same concept multiple times in the same form
		//Obs o = context.removeExistingObs(questionConcept, (Concept) null);
		
		defaultTool = parameters.get("defaultTool");
		
		//if no mode was specified, or using signature mode, 
		//default to path tool, while it's not strictly required as default
		//for annotation mode, it currently should be for signature mode
		if (defaultTool == null || instancePurpose.equals(DisplayMode.Signature)) {
			defaultTool = "path-tool";
		}
		
	}
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		
		if (required) {
			//TODO required attr support design and impl.
			//check that there are some elements like path, or that the submission
			//differs from the original svgTemplate that was sent out in generateHtml
			//hopefully neither the browser or frontend dev changed the DOM?
		}
		return null;
	}
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		
		FormSubmissionActions actions = session.getSubmissionActions();
		
		String svgDOM = submission.getParameter("svgDOM");
		
		//only create a "new" obs if this parameter is posted
		
		//loaded complex data is a trigger for dirty obs
		//due to the existing behavior of openmrs voiding
		//when saving "dirty obs" this is required or openmrs deletes the file
		
		AnnotatedImage ai = null;
		
		if (svgDOM == null || StringUtils.isBlank(svgDOM)) {
			
			String markup = new String((byte[]) parentObs.getComplexData().getData());
			
			ai = new AnnotatedImage(markup);
			
		} else {
			ai = new AnnotatedImage(svgDOM);
		}
		
		if (ai.getParsingError() != DrawingUtil.ErrorStatus.NONE)
			throw new RuntimeException("Error parsing the SVG document" + ai.getParsingError());
		
		byte[] svgByteArray = DrawingUtil.documentToString(ai.getImageDocument()).getBytes();
		
		try {
			
			//TODO verify this update pipeline works, and behaviors between
			//both update pipelines match
			if (session.getContext().getMode() == Mode.EDIT && parentObs != null) {
				
				actions.modifyObs(parentObs, questionConcept,
				    new ComplexData(DrawingConstants.BASE_COMPLEX_OBS_FILENAME, svgByteArray), null, null);
				
			} else {
				
				//this is the first version of this module that supports versioning,
				//so it will note this is the "1st version"... this version
				//does not directly map to the module version and may not change with
				//subsequent module versions, but could if more information is added, removed
				//or how the information is stored changes
				
				actions.createObs(questionConcept, new ComplexData(DrawingConstants.BASE_COMPLEX_OBS_FILENAME, svgByteArray),
				    null, null);
			}
			
		}
		catch (Exception e) {
			log.error("cannot create obs :" + e.getMessage(), e);
			throw new RuntimeException("Unable to save complex Observation!");
		}
		
	}
	
	//add one or more classes to an element
	private void addClasses(Element elem, String classes) {
		String prevClasses = elem.getAttribute("class");
		elem.setAttribute("class", prevClasses + " " + classes);
	}
	
	private void hideElement(String id, Document doc) {
		
		Element elem = DrawingUtil.getElementById(id, doc);
		
		if (elem != null) {
			addClasses(elem, "disabled hidden");
		}
		
	}
	
	/**
	 *
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		
		String css = "<link rel=\"stylesheet\" href=\"../../moduleResources/drawing/style.css\"/>";
		
		//TODO for now, during testing if the file can't be loaded, return early
		if (svgWidgetTemplate == null) {
			return "<span style='color:red;'>Error loading drawing element</span>";
		}
		
		//always hide save button when using editor in HFE forms, the submit button will
		//be provided
		hideElement("save-image", svgWidgetTemplate);
		
		//exclude any other explicitly excluded buttons by html id attr
		if (excludedButtonIds != null) {
			for (String id : excludedButtonIds) {
				hideElement(id, svgWidgetTemplate);
			}
		}
		
		if (svgWidgetTemplate != null) {
			Element rootSvg = DrawingUtil.getElementById("root-svg", svgWidgetTemplate);
			
			//TODO patch up widgetTemplate using DOM methods
			//1. make all ids (hopefully) unique (html form entry unique id?)
			//2. handle different classes (e.g. signature vs. annotated image)
			
			Mode ctxMode = context.getMode();
			
			//TODO add translated messages, parse HTML/XML template or provide
			//to JS?
			//DrawingUtil.translateLanguageKey("drawing.save");
			
			//in both view and edit, load the existing svg if it exists
			if ((ctxMode == Mode.VIEW || ctxMode == Mode.EDIT) && parentObs != null
			        && parentObs.getComplexData().getData() != null) {
				
				AnnotatedImage ai = new AnnotatedImage(new String((byte[]) parentObs.getComplexData().getData()));
				
				//TODO after SVG DOM insertion is implemented, handle this section appropriately
				Document svgDoc = ai.getImageDocument();
				
				Node storedSvg = svgDoc.getFirstChild();
				storedSvg = svgWidgetTemplate.importNode(storedSvg, true);
				rootSvg.getParentNode().replaceChild(storedSvg, rootSvg);
				rootSvg = (Element) storedSvg;
				
				//in view mode, disable interaction
				if (ctxMode == Mode.VIEW) {
					
					//in view mode only allow select
					defaultTool = "select-tool";
					
					//set disabled on all buttons
					
					rootSvg.getAttribute("class");
					
					NodeList buttons = svgWidgetTemplate.getElementsByTagName("button");
					//Element clearAllButton = svgWidgetTemplate.getElementById("");
					
					for (int i = 0; i < buttons.getLength(); i++) {
						
						Element button = (Element) buttons.item(i);
						
						addClasses(button, "hidden disabled");
						
					}
					
					//hide layers
					Element layerArrangeDiv = DrawingUtil.getElementById("arrange", svgWidgetTemplate);
					
					String buttonClasses = layerArrangeDiv.getAttribute("class");
					layerArrangeDiv.setAttribute("class", buttonClasses + " hidden");
					
				}
				
			} else if (ctxMode == Mode.ENTER || parentObs.getComplexData().getData() == null) {
				
				//the image only needs to be added if this is a new form being entered
				if (base64preload != null) {
					//add new child <image> to root-svg
					Element imageNode = svgWidgetTemplate.createElement("image");
					
					imageNode.setAttribute("xlink:href", base64preload);
					imageNode.setAttribute("data-ignore-layer", "true");
					imageNode.setAttribute("width", defaultViewportWidth);
					imageNode.setAttribute("height", defaultViewportHeight);
					
					rootSvg.appendChild(imageNode);
					rootSvg.setAttribute("width", defaultViewportWidth);
					rootSvg.setAttribute("height", defaultViewportHeight);
				}
				
			}
			
			rootSvg.setAttribute("data-default-tool", defaultTool);
			
			//these elements will be used to set explicit heights or visiblity for
			//different purposes
			Element parentDiv = DrawingUtil.getElementById("view-layer-parent", svgWidgetTemplate);
			Element layerParentDiv = DrawingUtil.getElementById("layer-div", svgWidgetTemplate);
			Element layerArrangeDiv = DrawingUtil.getElementById("arrange", svgWidgetTemplate);
			
			switch (instancePurpose) {
				case Annotation:
					//populate templates
					/*
					String[] encodedTemplateNames = DrawingUtil.getAllTemplateNames();
					if (encodedTemplateNames.length > 0) {
						sb.append("<div style='position:relative'>");
						sb.append("<div style='width:30%;height:100%;float:left;border:1px;;margin-bottom:10px'>");
						sb.append("<b class='boxHeader'>"+mss.getMessage("drawing.availableTemplates")+"</b>");
						sb.append("<div class='box' style='height:350px'>");
						sb.append(mss.getMessage("uicommons.search")+":<input type='search' id='searchTemplates' placeholder='search...'/>");
						sb.append("<div style='overflow-y: scroll;overflow-x:hidden;height:315px'>");
						sb.append("<table>");
						for (String encodedTemplateName : encodedTemplateNames) {
							sb.append("<tr>");
							sb.append("<td style='display:list-item;list-style:disc inside;'></td>");
							sb.append("<td class='templateName' style='cursor:pointer'>"
									+ encodedTemplateName + "</td>");
							sb.append("</tr>");
						}
						sb.append("</table>");
						sb.append("</div>");
						sb.append("</div>");
						sb.append("</div>");
						sb.append("<div style='float:left;width:68%;margin-left:10px;margin-bottom:10px' >");
						sb.append("<b class='boxHeader'>"+mss.getMessage("drawing.preview")+"</b>");
						sb.append("<div class='box' style='height:350px'>");
						sb.append("<img  src='/"
								+ WebConstants.WEBAPP_NAME
								+ "/moduleResources/drawing/images/preview.png' id='templateImage"
								+ id + "' class='templateImage'/>");
					
						sb.append("</div>");
						sb.append("</div>");
					
						sb.append("</div>");
						sb.append("<div style='clear:both'></div>");
					} else {
						sb.append(mss.getMessage("drawing.noTemplatesUploaded"));
					}
					*/
					
					break;
				
				case Signature:
					//hide all buttons except clear all
					NodeList buttons = svgWidgetTemplate.getElementsByTagName("button");
					//Element clearAllButton = svgWidgetTemplate.getElementById("");
					
					for (int i = 0; i < buttons.getLength(); i++) {
						
						Element button = (Element) buttons.item(i);
						
						if (button.getAttribute("id").equals("clear-all")) {
							//skip setting display:none
							continue;
						}
						
						addClasses(button, "hidden");
						
					}
					
					String buttonClasses = layerArrangeDiv.getAttribute("class");
					layerArrangeDiv.setAttribute("class", buttonClasses + " hidden");
					
					break;
				
				default:
					throw new RuntimeException("Invalid <drawing> displayMode");
					
			}
			
			Element[] fixedHeightDivs = { parentDiv, layerParentDiv, layerArrangeDiv };
			css += "<style>";
			for (Element elem : fixedHeightDivs) {
				
				if (elem != null) {
					String elemCss = "#" + elem.getAttribute("id") + " {";
					
					if (elem == parentDiv && fixedWidth != null) {
						elemCss += "width:" + fixedWidth + ";";
					}
					
					if (fixedHeight != null) {
						elemCss += "height:" + fixedHeight + ";";
					}
					
					css += elemCss + "}";
				}
				
			}
			css += "</style>";
		}
		
		//add a hidden input to provide the SVG on submission
		
		//<input type="hidden" id="svgDOM" name="svgDOM" value="${svgDOM}"/>
		Element svgDomInput = svgWidgetTemplate.createElement("input");
		svgDomInput.setAttribute("name", "svgDOM");
		svgDomInput.setAttribute("id", "svgDOM");
		svgDomInput.setAttribute("type", "hidden");
		
		//since the DOM is manipulated directly when generating HTML here,
		//in this case the value is "" 
		svgDomInput.setAttribute("value", "");
		
		svgDomInput.appendChild(svgWidgetTemplate.createComment("expand tag for valid xml"));
		
		svgWidgetTemplate.getFirstChild().appendChild(svgDomInput);
		
		String widgetMarkup = DrawingUtil.bodyChildrenToString(svgWidgetTemplate);
		
		if (widgetMarkup == null || widgetMarkup.isEmpty()) {
			widgetMarkup = "<span style='color:red;'>Error loading drawing element</span>";
		}
		
		//If the file that contains the drawing for some reason dont exists, show an error message and send a new drawing.
		if (parentObs != null && parentObs.getComplexData().getData() == null && context.getMode() != Mode.ENTER) {
			widgetMarkup = "<span style='color:red;'>"
			        + Context.getMessageSourceService().getMessage("drawing.error.getting.drawing.file") + "</span>"
			        + widgetMarkup;
		}
		
		String js = "<script src=\"../../moduleResources/drawing/svg.js\"></script>"
		        + "<script src=\"../../moduleResources/drawing/svg.draw.js\"></script>"
		        + "<script src=\"../../moduleResources/drawing/ui.js\"></script>";
		
		return css + widgetMarkup + js;
	}
}

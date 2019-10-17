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
package org.openmrs.module.drawing.obs.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
//import org.openmrs.module.drawing.AnnotatedImage;
import org.openmrs.module.drawing.DrawingConstants;
import org.openmrs.module.drawing.DrawingUtil;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.handler.TextHandler;
import org.openmrs.web.WebConstants;
import org.openmrs.obs.ComplexObsHandler;

/**
 *
 */
public class DrawingHandler extends TextHandler {
	
	private Log log = LogFactory.getLog(DrawingHandler.class);
	
	private static final String[] viewModes = { ComplexObsHandler.RAW_VIEW, ComplexObsHandler.TEXT_VIEW,
	        ComplexObsHandler.HTML_VIEW, ComplexObsHandler.URI_VIEW };
	
	/**
	 * @see org.openmrs.obs.handler.ImageHandler#saveObs(org.openmrs.Obs)
	 * @see org.openmrs.obs.handler.TextHandler#saveObs(org.openmrs.Obs)
	 */
	@Override
	public Obs saveObs(Obs obs) {
		ComplexData c = obs.getComplexData();
		
		if (c == null) {
			log.error("Cannot save complex data where obsId=" + obs.getObsId() + " because its ComplexData is null.");
			return obs;
		}
		
		Object dataObject = c.getData();
		if (dataObject == null) {
			log.error("Cannot save complex data where obsId=" + obs.getObsId() + " because its ComplexData data is null.");
			return obs;
		}
		
		Object svgText = null;
		
		//convert byte[] to char[] if necessary
		//pass through TextHandler supported data classes
		if (dataObject instanceof byte[]) {
			
			byte[] data = (byte[]) dataObject;
			svgText = new String(data).toCharArray();
			
		} else if (dataObject instanceof char[] || Reader.class.isAssignableFrom(dataObject.getClass())
		        || InputStream.class.isAssignableFrom(dataObject.getClass())) {
			
			svgText = dataObject;
			
		} /*else if (dataObject.getClass() == String.class) {
		  
		  svgText = ((String) dataObject).toCharArray();
		  }*/
		
		if (svgText == null) {
			log.error("Cannot save complex data where obsId=" + obs.getObsId()
			        + " because its ComplexData is not a supported format.");
			return obs;
		}
		
		//TextHandler expects complex data to be char[], reader or inputstream, but images are
		//expected to take byte []
		
		ComplexData svgComplexData = new ComplexData(DrawingConstants.BASE_COMPLEX_OBS_FILENAME, svgText);
		
		svgComplexData.setMimeType("image/svg+xml");
		
		obs.setComplexData(svgComplexData);
		
		Obs o = super.saveObs(obs);
		
		//But RAW_VIEW for this MimeType is expected
		//to be String complex data, and
		
		//if this same observation instance will be used by other code immediately,
		//instead of being retrieved again later using getObs,
		//call getObs to make sure it has a reference to the complex data
		//that was just saved and it is a RAW_VIEW
		
		//this currently occurs during testing with HFE.RegressionTestHelper
		//which has identical symptoms to an issue encountered when saving
		//the complex obs in an obs group
		
		//TODO evaluate if this is similar to what's happening
		//in the obs group issue, and this also resolves that issue
		
		log.info("drawing:saving complexObs:" + o);
		
		//this makes sure the obs is ready to use, but is less performant
		//and goes beyond the design expectations judging by existing code comments
		//it is less intuitive, but without it do the tests more accurately reflect
		//the expected usage by other devs?
		//o = getObs(o, ComplexObsHandler.RAW_VIEW);
		
		return o;
	}
	
	public Obs getObs(Obs obs, String view) {
		File svgFile = getComplexDataFile(obs);
		
		//not loading this into the annotated image wont verify the file is valid
		//but since the user may not use annotated image or may just 
		//try loading it annotated again, this is probably a good change
		
		//AnnotatedImage aimage = new AnnotatedImage(svgFile);
		
		//this would avoid setting dirty on the original obs.... but is not how
		//some of the current core/HFE code seems to work, 
		//other Complex Obs handlers e.g. TextHandler just update the obs passed in
		
		//Obs newObs = Obs.newInstance(originalObs);
		//newObs.setComplexData(complexData);
		
		String url = "/" + WebConstants.WEBAPP_NAME + "/module/drawing/manage.form?obsId=" + obs.getId();
		if (view == ComplexObsHandler.URI_VIEW) {
			obs.setComplexData(new ComplexData(svgFile.getName(), url));
		} else if (view == ComplexObsHandler.HTML_VIEW) {
			String html = "<a href=\"" + url + "\">" + svgFile.getName() + "</a>";
			obs.setComplexData(new ComplexData(svgFile.getName(), html));
		} else if (view == ComplexObsHandler.TEXT_VIEW) {
			String data = "";
			
			try {
				data = DrawingUtil.loadResourceServerside(svgFile.getAbsolutePath());
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			obs.setComplexData(new ComplexData(svgFile.getName(), data));
		} else {
			
			byte[] data = null;
			
			try {
				String markup = DrawingUtil.loadResourceServerside(svgFile.getAbsolutePath());
				
				data = markup.getBytes();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			ComplexData cd = new ComplexData(svgFile.getName(), data);
			
			//store the mime type
			cd.setMimeType("image/svg+xml");
			obs.setComplexData(cd);
		}
		
		return obs;
	}
	
	@Override
	public String[] getSupportedViews() {
		return viewModes;
	}
	
	/**
	 * Parses the XML metadata file (if it exists) loads the metadata into the given AnnotatedImage and
	 * returns it.
	 * 
	 * @param obs
	 */
	//TODO Eval existing module usage to determine priority/need
	//for supporting reading legacy 1.1 files and updating to 1.2 svg
	/*	public AnnotatedImage loadMetadata(Obs obs, AnnotatedImage image) {
			
			File metadataFile = getComplexMetadataFile(obs);
			
			image.setHandler(this);
			
			ArrayList<ImageAnnotation> annotations = new ArrayList<ImageAnnotation>();
			if (metadataFile.exists() && metadataFile.canRead()) {
				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document xmldoc = builder.parse(metadataFile);
					NodeList annotationNodeList = xmldoc.getElementsByTagName("Annotation");
					
					for (int i = 0; i < annotationNodeList.getLength(); i++) {
						try {
							Node node = annotationNodeList.item(i);
							NamedNodeMap attributes = node.getAttributes();
							String text = node.getTextContent();
							String idString = attributes.getNamedItem("id").getNodeValue();
							String date = attributes.getNamedItem("date").getNodeValue();
							String userid = attributes.getNamedItem("userid").getNodeValue();
							String xcoordinate = attributes.getNamedItem("xcoordinate").getNodeValue();
							String ycoordinate = attributes.getNamedItem("ycoordinate").getNodeValue();
							Position position = new Position(Integer.parseInt(xcoordinate), Integer.parseInt(ycoordinate));
							User user = Context.getUserService().getUser(Integer.parseInt(userid));
							annotations.add(new ImageAnnotation(Integer.parseInt(idString), position, text, new Date(Long
							        .parseLong(date)), user, Status.UNCHANGED));
						}
						catch (NumberFormatException e) {
							// Skip that annotation
						}
					}
					
				}
				catch (Exception e) {
					//Likely ParserConfigurationException, SAXException or IOException.
					//Fail silently, log the error and return the image with no annotations.
					log.error("Error loading annotations", e);
				}
			}
			image.setAnnotations(annotations.toArray(new ImageAnnotation[0]));
			
			return image;
		}
	
		//TODO Eval existing module usage to determine priority/need
		//for supporting reading legacy 1.1 files and updating to 1.2 svg
		public void saveAnnotation(Obs obs, ImageAnnotation annotation, boolean delete) {
			try {
				log.info("drawing: Saving annotation for obs " + obs.getObsId());
				
				File metadataFile = getComplexMetadataFile(obs);
				log.info("drawing: Using file " + metadataFile.getCanonicalPath());
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document xmldoc;
				Element annotationsParent;
				int newId = 0;
				
				if (metadataFile.exists()) {
					xmldoc = builder.parse(metadataFile);
					annotationsParent = (Element) xmldoc.getElementsByTagName("Annotations").item(0);
					NodeList annotationNodeList = xmldoc.getElementsByTagName("Annotation");
					
					for (int i = 0; i < annotationNodeList.getLength(); i++) {
						NamedNodeMap attributes = annotationNodeList.item(i).getAttributes();
						String idString = attributes.getNamedItem("id").getNodeValue();
						int existingId = Integer.parseInt(idString);
						if (existingId == annotation.getId() && !(annotation.getStatus() == Status.UNCHANGED)) {
							annotationsParent.removeChild(annotationNodeList.item(i));
							break;
						}
						if (existingId >= newId)
							newId = existingId + 1;
					}
				} else {
					metadataFile.createNewFile();
					DOMImplementation domImpl = builder.getDOMImplementation();
					xmldoc = domImpl.createDocument(null, "ImageMetadata", null);
					Element root = xmldoc.getDocumentElement();
					annotationsParent = xmldoc.createElementNS(null, "Annotations");
					root.appendChild(annotationsParent);
				}
				
				if (!delete) {
					if (annotation.getId() >= 0)
						newId = annotation.getId();
					
					Element e = xmldoc.createElementNS(null, "Annotation");
					Node n = xmldoc.createTextNode(annotation.getText());
					e.setAttributeNS(null, "id", newId + "");
					e.setAttributeNS(null, "xcoordinate", annotation.getLocation().getX() + "");
					e.setAttributeNS(null, "ycoordinate", annotation.getLocation().getY() + "");
					e.setAttributeNS(null, "userid", annotation.getUser().getUserId() + "");
					e.setAttributeNS(null, "date", annotation.getDate().getTime() + "");
					e.appendChild(n);
					annotationsParent.appendChild(e);
				}
				
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.transform(new DOMSource(xmldoc), new StreamResult(metadataFile));
				
				log.info("drawing: Saving annotation complete");
				
			}
			catch (Exception e) {
				log.error("drawing: Error saving image metadata: " + e.getClass() + " " + e.getMessage());
			}
		}
	*/
	/**
	 * Convenience method to create and return a file for the stored metadata file
	 * 
	 * @param obs
	 * @return
	 */
	
	//TODO Eval existing module usage to determine priority/need
	//for supporting reading legacy 1.1 files and updating to 1.2 svg
	/*	public static File getComplexMetadataFile(Obs obs) {
		File imageFile = ImageHandler.getComplexDataFile(obs);
		try {
			return new File(imageFile.getCanonicalPath() + ".xml");
		}
		catch (IOException e) {
			return new File(imageFile.getAbsolutePath() + ".xml");
		}
	}
	*/
}

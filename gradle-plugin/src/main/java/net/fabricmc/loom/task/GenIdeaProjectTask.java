/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.task;

import org.gradle.api.DefaultTask;

public class GenIdeaProjectTask extends DefaultTask {
	/*@TaskAction
	public void genIdeaRuns(Project project) throws IOException, ParserConfigurationException, SAXException, TransformerException {
		// Only generate the idea runs on the root project
		if (project.getRootProject() != project) {
			return;
		}

		project.getLogger().lifecycle(":Building idea workspace");
		File file = project.file(project.getName() + ".iws");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file);

		NodeList list = doc.getElementsByTagName("component");
		Element runManager = null;

		for (int i = 0; i < list.getLength(); i++) {
			Element element = (Element) list.item(i);

			if (element.getAttribute("name").equals("RunManager")) {
				runManager = element;
				break;
			}
		}

		if (runManager == null) {
			throw new RuntimeException("Failed to generate IntelliJ run configurations (runManager was not found)");
		}

		BaseAmalgamation amalgamation = (BaseAmalgamation) project.getExtensions().getByName("ag");

		for (RunConfigSettings settings : amalgamation.getRunConfigs()) {
			if (!settings.isIdeConfigGenerated()) {
				continue;
			}

			runManager.appendChild(RunConfig.runConfig(project, settings).genRuns(runManager));
			settings.makeRunDir();
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.transform(source, result);
	}*/
}
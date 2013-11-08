package edu.tongji.putin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class cssToolShowCart2 {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File dir = new File("D:\\html");
					Tool.initlist();
					Tool.process(dir);					
	}

	private static class Tool {
		private static ArrayList<File> htmlfiles = new ArrayList<File>();
		private static ArrayList<MyFile> myfiles = new ArrayList<MyFile>();
		private static ArrayList<CssBlock> cssBlocks = new ArrayList<cssToolShowCart2.CssBlock>();
		private static ArrayList<Label> contentlabels = null;
		private static String dirname = "";
		private static File cssgen = new File("D:/html/gen_webpro.css");

		private static Reader reader = null;
		private static Writer writer = null;
		private static File htmlfile = null;
		private static StringBuffer content = null;
		private static ArrayList<RawSb> detailcontent = null;
		private static String contentStr = null;
		private static String prefix = null;

		public static void initlist(){
			htmlfiles = new ArrayList<File>();
			myfiles = new ArrayList<MyFile>();
			cssBlocks = new ArrayList<cssToolShowCart2.CssBlock>();
			contentlabels = new ArrayList<cssToolShowCart2.Label>();
			dirname = "";
		}
		
		public static void init(File htmlfile) {
			Tool.htmlfile = htmlfile;
			if (htmlfile.canRead()) {
				prefix = "showCart";
				content = new StringBuffer();
				detailcontent = new ArrayList<RawSb>();
			} else {
				System.out.println("can not read" + htmlfile.getName());
			}
			Label.tablecount = 1;
			Label.tdcount = 1;
		}

		private static void process(File dir) {
			readprocess(dir);
			middleprocess();
			writeprocess();
		}

		private static void readprocess(File dir) {
			if (dir.isDirectory()) {
				dirname = dir.getName();
				File[] files = dir.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isFile()
							&& files[i].getName().split("\\.")[1]
									.equals("p")) {
						htmlfiles.add(files[i]);
					}
				}
				for (int i = 0; i < htmlfiles.size(); i++) {
					init(htmlfiles.get(i));
					readFile();
					pulloutCss();
				}
			} else {
				System.out.println("not dir");
			}
		}

		private static void middleprocess() {
			formatLable();
			combineCssBlocks();
			gennewClass();
		}

		private static void writeprocess() {
			replaceOldLabel();
			writehtmlfiles();
			writecssfile();
		}

		private static void gennewClass() {
			int count = 1;
			boolean flag = false;
			boolean hastd = false;
			for(CssBlock css : cssBlocks){
				String classname = "showCart" + count;
				hastd = false;
				for(String selector : css.getSelector()){
					String id = selector.split("#")[1];//wrong! when table#xxx td
					if(id.trim().contains(" ")){
						id = id.split(" ")[0];
						hastd = true;
					}
					for(Label label : contentlabels){
						if(label.getId().equals(id)){
							flag = false;
							for(Attr attr : label.getAttributes()){
								if(attr.name.equals("class")){
									attr.setValue(attr.getValue() + " " + classname);
									flag = true;
									break;
								}
							}
							if(!flag){
								label.addAttr(new Attr("class", classname));
							}
							break;
						}
					}
				}
				if(hastd){
					css.setClassSelector("." + classname+ " td");	
				}else{
					css.setClassSelector("." + classname);					
				}
				count++;
			}
		}

		private static void combineCssBlocks() {
			CssBlock css = null;
			for (int i = 0; i < cssBlocks.size(); i++) {
				css = cssBlocks.get(i);
				for (int j = 1; i + j < cssBlocks.size(); j++) {
					if (css.equalsAttr(cssBlocks.get(i + j))) {
						css.addSelector(cssBlocks.get(i + j).getSelector());
						cssBlocks.remove(i + j);
						j--;
					}
				}
			}
		}

		private static void readFile() {
			int tempint;
			try {
				reader = new InputStreamReader(new FileInputStream(htmlfile));
				while ((tempint = reader.read()) != -1) {
					content.append((char) tempint);
				}
				reader.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("error when reading html files");
			}
			String tempContentStr = content.toString();

			RawSb rsb = new RawSb();
			rsb.setHasprefix(true);
			rsb.setSb(tempContentStr);
			detailcontent.add(rsb);
		}

		private static void pulloutCss() {
			String escapechar = "";
			for (RawSb rsb : detailcontent) {
				if (rsb.isHasprefix()) {
					escapechar = "~\"";
				} else {
					escapechar = "\"";
				}
				createLabels(rsb.getSb(), "<table", escapechar);
				createLabels(rsb.getSb(), "<td", escapechar);
			}
			content = new StringBuffer();

			for (RawSb sb : detailcontent) {
				content.append(sb.getSb());
			}

			myfiles.add(new MyFile(htmlfile, content.toString()));
		}

		private static void formatLable() {
			CssBlock css = null;
			boolean addPadding = false;
			String padding = null;
			for (int i = 0; i < contentlabels.size(); i++) {
				css = new CssBlock();
				Label label = contentlabels.get(i);
				css.addOneSelector(label.getName().toLowerCase() + "#"
						+ label.getPid().substring(1));

				int attrsize = label.getAttributes().size();
				for (int j = 0; j < attrsize; j++) {
					Attr attr = label.getAttributes().get(j);
					if (attr.getName().toLowerCase().equals("width")) {
						if (!attr.getValue().contains("%")
								&& !attr.getValue().contains("px")
								&& !attr.getValue().contains("em")) {
							attr.setValue(attr.getValue().trim().concat("px"));
						}
						css.addAttr("width", attr.getValue());
					} else if (attr.getName().toLowerCase().equals("align")) {
						css.addAttr("text-align", attr.getValue().toLowerCase());
					} else if (attr.getName().toLowerCase().equals("valign")) {
						css.addAttr("vertical-align", attr.getValue()
								.toLowerCase());
					} else if (attr.getName().toLowerCase().equals("border")) {
						if (!attr.getValue().contains("%")
								&& !attr.getValue().contains("px")
								&& !attr.getValue().contains("em")) {
							attr.setValue(attr.getValue().trim().concat("px"));
						}
						css.addAttr("border", attr.getValue());
					} else if (attr.getName().toLowerCase()
							.equals("cellpadding")) {
						addPadding = true;
						padding = attr.getValue();
					} else if (attr.getName().toLowerCase()
							.equals("cellspacing")) {
						if (!attr.getValue().contains("%")
								&& !attr.getValue().contains("px")
								&& !attr.getValue().contains("em")) {
							attr.setValue(attr.getValue().trim().concat("px"));
						}
						css.addAttr("border-spacing", attr.getValue());
						css.addAttr("border-collapse", "collapse");
					} else if (attr.getName().toLowerCase().equals("style")) {
						if (attr.getValue().indexOf(";") == -1) {
							attr.setValue(attr.getValue() + ";");
						}
						for (String str : attr.getValue().split(";")) {
							css.addAttr(str.split(":")[0].toLowerCase(),
									str.split(":")[1].toLowerCase());
						}
					} else if (attr.getName().toLowerCase().equals("nowrap")) {
						css.addAttr("white-space", "nowrap");
					} else if (attr.getName().toLowerCase().equals("bgcolor")) {
						css.addAttr("background-color", attr.getValue());
					}
				}

				cssBlocks.add(css);

				CssBlock csspadding = new CssBlock();

				if (addPadding) {
					if (!padding.contains("%") && !padding.contains("px")
							&& !padding.contains("em")) {
						padding = padding.trim().concat("px");
					}
					csspadding.addOneSelector(label.getName() + "#"
							+ label.getId() + " td");
					csspadding.addAttr("padding", padding);
					cssBlocks.add(csspadding);
					addPadding = false;
				}
			}

		}


		private static void writehtmlfiles() {
			for (MyFile file : myfiles) {

				try {
					file.getHtmlfile().createNewFile();
					Writer htmlwriter = new OutputStreamWriter(
							new FileOutputStream(file.getHtmlfile()));
					htmlwriter.write(file.getContentStr());
					htmlwriter.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Can not write html file");
				}
			}
		}

		private static void writecssfile() {
			try {
				if (!cssgen.exists()) {
					cssgen.createNewFile();
				}
				writer = new OutputStreamWriter(new FileOutputStream(cssgen,
						true));
				for (CssBlock css : cssBlocks) {
					writer.append(css.getSelectorString() + "{\n");
					for (Entry<String, String> attr : css.getAttributes()
							.entrySet()) {
						writer.append(attr.getKey() + ":" + attr.getValue()
								+ ";\n");
					}
					writer.append("}\n");
				}
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Can not write css file");
			} finally {

			}
		}

		private static void replaceOldLabel() {
			String replaceStr = "";
			
			for (int i = 0; i < contentlabels.size(); i++) {
				Label label = contentlabels.get(i);
				replaceStr = "<" + label.getName();
				String ec = label.getEscapechar();
				replaceStr = replaceStr.concat(" id=" + ec
						+ label.getId() + ec);
				for (int j = 0; j < label.getAttributes().size(); j++) {
					Attr tempattr = label.getAttributes().get(j);
					if (tempattr.getName().toLowerCase().equals("class")) {
						replaceStr = replaceStr.concat(" class=" + ec
								+ tempattr.getValue() + ec);
					} else if (tempattr.getName().toLowerCase().equals("colspan")) {
						replaceStr = replaceStr.concat(" colspan=" + ec
								+ tempattr.getValue() + ec);
					}
				}
				
				for(MyFile mf : myfiles){
					String str = mf.getContentStr();
					if(str.contains(label.getPid())){
						mf.setContentStr(str.replace(label.getPid(), replaceStr));
						break;
					}
				}
			}
		}

		private static void createLabels(StringBuffer content,
				String lablename, String escapechar) {
			int indexl = content.indexOf(lablename.toLowerCase());
			int indexh = content.indexOf(lablename.toUpperCase());
			while (indexl != -1 || indexh != -1) {
				int start, end;
				if (indexl == -1) {
					start = indexh;
				} else if (indexh == -1) {
					start = indexl;
				} else {
					start = indexl < indexh ? indexl : indexh;
				}

				end = content.indexOf(">", start);

				Label tempLabel = new Label(content.substring(start, end),
						prefix, escapechar);
				contentlabels.add(tempLabel);
				content.replace(start, end, tempLabel.getPid());

				indexl = content.indexOf(lablename.toLowerCase());
				indexh = content.indexOf(lablename.toUpperCase());
			}
		}
	}

	private static class MyFile {
		private File htmlfile;
		private String contentStr;

		public MyFile(File file, String str) {
			this.htmlfile = file;
			this.contentStr = str;
		}

		public File getHtmlfile() {
			return htmlfile;
		}

		public String getContentStr() {
			return contentStr;
		}
		
		public void setContentStr(String contentStr) {
			this.contentStr = contentStr;
		}

	}

	private static class RawSb {
		private StringBuffer sb = null;
		private boolean hasprefix = false;

		public StringBuffer getSb() {
			return sb;
		}

		public void setSb(String str) {
			this.sb = new StringBuffer(str);
		}

		public boolean isHasprefix() {
			return hasprefix;
		}

		public void setHasprefix(boolean hasprefix) {
			this.hasprefix = hasprefix;
		}
	}

	private static class CssBlock {
		private ArrayList<String> selector = new ArrayList<String>();
		private Map<String, String> attributes = new HashMap<String, String>();

		public Map<String, String> getAttributes() {
			return attributes;
		}

		public void addAttr(String name, String value) {
			attributes.put(name, value);
		}

		public String getSelectorString() {
			String str = "";
			String temp = null;
			for (int i = 0; i < selector.size(); i++) {
				temp = selector.get(i);
				if (i != selector.size() - 1) {
					str += temp + ",\n";
				} else {
					str += temp;
				}
			}
			return str;
		}

		public ArrayList<String> getSelector() {
			return selector;
		}

		public void addSelector(ArrayList<String> newselector) {
			this.selector.addAll(newselector);
		}
		
		public void setClassSelector(String selector){
			this.selector = new ArrayList<String>();
			this.selector.add(selector);
		}

		public void addOneSelector(String newselector) {
			this.selector.add(newselector);
		}

		public boolean equalsAttr(CssBlock css) {
			if (css.getAttributes().size() != this.attributes.size()) {
				return false;
			}
			for (Entry<String, String> entry : css.getAttributes().entrySet()) {
				String key = entry.getKey();
				if (this.getAttributes().get(key) == null) {
					return false;
				} else {
					if (!this.getAttributes().get(key).equals(entry.getValue())) {
						return false;
					}
				}
			}
			return true;
		}

	}

	private static class Label {
		private static int tablecount = 1;
		private static int tdcount = 1;
		private String pid;
		private String id;
		private String name;
		private ArrayList<Attr> attributes = new ArrayList<cssToolShowCart2.Attr>();
		private String escapechar = null;

		public Label(String str, String prefix, String escapechar) {
			this.setEscapechar(escapechar);
			StringBuffer tempsb = new StringBuffer();
			tempsb.append(str);
			int start = 0;
			int end = -1;
			start = tempsb.indexOf("<");
			for (int i = start + 1; i < tempsb.length(); i++) {
				if (Character.isWhitespace(tempsb.charAt(i))) {
					end = i;
					break;
				}
			}
			if (end != -1) {
				this.name = tempsb.substring(start + 1, end).trim()
						.toLowerCase();
				if (this.name.equals("table")) {
					this.pid = "<" + prefix + this.name + tablecount + "#";
					this.setId(prefix + this.name + tablecount);
					tablecount++;
				} else {
					this.pid = "<" + prefix + this.name + tdcount + "#";
					this.setId(prefix + this.name + tdcount);
					tdcount++;
				}
				tempsb.delete(start, end + 1);
				if (!tempsb.toString().trim().isEmpty()) {
					genAttributes(tempsb.toString());
				}
			} else {
				this.name = tempsb.substring(start + 1).trim().toLowerCase();
				if (this.name.equals("table")) {
					this.pid = "<" + prefix + this.name + tablecount + "#";
					this.setId(prefix + this.name + tablecount);
					tablecount++;
				} else {
					this.pid = "<" + prefix + this.name + tdcount + "#";
					this.setId(prefix + this.name + tdcount);
					tdcount++;
				}
			}
		}

		private String removeChar(String str, String strChar) {
			StringBuffer sb = new StringBuffer(str);
			while (sb.indexOf(strChar) != -1) {
				sb.deleteCharAt(sb.indexOf(strChar));
			}
			return sb.toString();
		}

		private void genAttributes(String str) {
			ArrayList<String> attrstr = new ArrayList<String>();
			str = str.replace("\t", " ");
			String[] arstr = str.split(" ");
			String[] temparstr;
			String tempstr;

			for (int i = 0; i < arstr.length; i++) {
				if (arstr[i].length() > 0) {
					attrstr.add(arstr[i]);
				}
			}

			for (int i = 1; i < attrstr.size(); i++) {
				if (!attrstr.get(i).contains("=")) {
					if (attrstr.get(i - 1).contains("=")
							&& attrstr.get(i - 1).indexOf("=") == attrstr.get(
									i - 1).length() - 1) {
						attrstr.set(i - 1,
								attrstr.get(i - 1).concat(attrstr.get(i)));
						attrstr.remove(i);
						i--;
					}
				}
			}

			for (int i = 0; i < attrstr.size(); i++) {
				if (attrstr.get(i).contains("=")) {
					if (attrstr.get(i).indexOf("=",
							attrstr.get(i).indexOf("=") + 1) != -1) {

						int beginIndex = attrstr.get(i).indexOf("\"",
								attrstr.get(i).indexOf("\"") + 1) + 1;
						if (attrstr.get(i).indexOf(
								"=",
								attrstr.get(i).indexOf("=",
										attrstr.get(i).indexOf("=") + 1) + 1) != -1) {
							System.out.println(this.pid);
						}
						attrstr.add(attrstr.get(i).substring(beginIndex));
						attrstr.set(i, attrstr.get(i).substring(0, beginIndex));
					}
				}
			}

			// mark
			for (int i = 0; i < attrstr.size(); i++) {
				int start = -1;
				int end = -1;
				if (attrstr.get(i).contains("=")
						&& attrstr.get(i).contains("\"")
						&& attrstr.get(i).indexOf("\"",
								attrstr.get(i).indexOf("\"") + 1) == -1) {
					start = i;
					for (int j = i + 1; j < attrstr.size(); j++) {
						if (!attrstr.get(j).contains("=")
								&& attrstr.get(j).contains("\"")
								&& attrstr.get(j).indexOf("\"",
										attrstr.get(j).indexOf("\"") + 1) == -1) {
							end = j;
							break;
						}
					}
					String tstr = "";
					for (int z = start; z <= end; z++) {
						tstr = tstr + attrstr.get(z) + " ";
					}
					int z = start + 1;
					for (int x = 1; x <= end - start; x++) {
						attrstr.remove(z);
					}

					attrstr.set(start, tstr);
				}
			}

			for (int i = 0; i < attrstr.size(); i++) {
				if (attrstr.get(i).length() > 0) {
					tempstr = attrstr.get(i);
					temparstr = tempstr.split("=");
					if (temparstr.length > 1) {
						String tempvalue = temparstr[1];
						tempvalue = removeChar(tempvalue, "~");
						tempvalue = removeChar(tempvalue, "\"");
						tempvalue = removeChar(tempvalue, "\'");
						tempvalue = removeChar(tempvalue, "`");
						Attr attribute = new Attr(temparstr[0].trim(),
								tempvalue.trim());
						this.attributes.add(attribute);
					} else {
						Attr attribute = new Attr(temparstr[0].trim(), null);
						this.attributes.add(attribute);
					}
				}
			}
		}
		
		public void addAttr(Attr attr){
			this.attributes.add(attr);
		}

		public String getPid() {
			return pid;
		}

		public String getName() {
			return name;
		}

		public ArrayList<Attr> getAttributes() {
			return attributes;
		}

		public void setEscapechar(String escapechar) {
			this.escapechar = escapechar;
		}

		public String getEscapechar() {
			return escapechar;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	private static class Attr {
		private String name;
		private String value;

		public Attr(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}

package edu.tongji.putin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class cssToolCombine {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File dir = new File("D:\\html");
		Tool.readCss();
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					Tool.initlist();
					Tool.process(files[i]);
				}
			}
		} else {
			System.out.println("not dir");
		}
	}

	private static class Tool {
		// application scope
		private static ArrayList<CssBlock> cssBlocksInFile = new ArrayList<cssToolCombine.CssBlock>();
		private static Map<String, CssBlock> cssBlocksInFileMap = new HashMap<String, cssToolCombine.CssBlock>();
		private static StringBuffer cssSb = null;
		private static File cssgen = new File("D:/html/gen_webpro.css");
		private static Reader reader = null;
		private static Writer writer = null;
		// directory scope
		private static ArrayList<File> pfiles = null;
		private static ArrayList<MyFile> myfiles = null;
		private static ArrayList<CssBlock> cssBlocks = null;
		private static ArrayList<Label> contentlabels = null;
		private static String dirname = "";
		// file scope
		private static File pfile = null;
		private static StringBuffer content = null;
		private static String prefix = null;
		private static String contentStr = null;

		public static void initlist() {
			pfiles = new ArrayList<File>();
			myfiles = new ArrayList<MyFile>();
			cssBlocks = new ArrayList<cssToolCombine.CssBlock>();
			contentlabels = new ArrayList<cssToolCombine.Label>();
			dirname = "";
			cssSb = new StringBuffer();
		}

		public static void init(File pfile) {
			Tool.pfile = pfile;
			if (pfile.canRead()) {
				prefix = pfile.getName().split("\\.")[0];
				content = new StringBuffer();
				contentStr = null;
			} else {
				System.out.println("can not read" + pfile.getName());
			}
			Label.tablecount = 1;
			Label.tdcount = 1;
		}

		private static void process(File dir) {
			readprocess(dir);
			middleprocess();
			// writeprocess();
		}

		private static void readCss() {
			int tempint;

			cssSb = new StringBuffer();
			try {
				reader = new InputStreamReader(new FileInputStream(cssgen));
				while ((tempint = reader.read()) != -1) {
					cssSb.append((char) tempint);
				}
				reader.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("error when reading gen_webpro.css");
			}

			int start = 0;
			int end = 0;
			String tempStr = "";
			while (cssSb.indexOf("{", start) != -1) {
				CssBlock css = new CssBlock();
				end = cssSb.indexOf("{", start);
				tempStr = cssSb.substring(start, end).trim();
				css.addOneSelector(tempStr.trim());
				String tempclassname = tempStr.split("\\.")[1].trim();
				if (tempclassname.contains(" ")) {
					css.setClassname(tempclassname.split(" ")[0]);
				} else {
					css.setClassname(tempclassname);
				}
				start = end + 1;
				if (cssSb.indexOf("}", start) != -1) {
					end = cssSb.indexOf("}", start);
					tempStr = cssSb.substring(start, end).trim();
					if (!tempStr.isEmpty()) {
						for (String str : tempStr.trim().split(";")) {
							css.addAttr(str.split(":")[0].trim(),
									str.split(":")[1].trim());
						}
					}
				} else {
					System.out.println("error1 in read css file");
				}
				cssBlocksInFile.add(css);
				cssBlocksInFileMap.put(css.getClassname(), css);
				start = end + 1;
			}
		}

		private static void readprocess(File dir) {
			if (dir.isDirectory()) {
				dirname = dir.getName();
				File[] files = dir.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						readprocess(files[i]);
					}
					if (files[i].isFile()
							&& (files[i].getName().split("\\.")[1].equals("p") || files[i]
									.getName().split("\\.")[1].equals("html"))) {
						pfiles.add(files[i]);
					}
				}
				for (int i = 0; i < pfiles.size(); i++) {
					init(pfiles.get(i));
					readFile();
					pulloutCss();
				}
			} else {
				System.out.println("not dir");
			}
		}

		private static void middleprocess() {
			// formatLable();
			// combineCssBlocks();
			// gennewClass();
			// addclassnames();
			combineClassNames();
		}

		private static void combineClassNames() {
			// TODO Auto-generated method stub

		}

		private static void addclassnames() {
			// TODO Auto-generated method stub
			for (CssBlock css : cssBlocksInFile) {
				for (String selector : css.getSelector()) {
					if (selector.contains(" td")) {
						if (selector.contains(".")) {
							findlableaddclass(
									selector.split(" ")[0].split("\\.")[1],
									css.getClassname());
						} else {
							findlableaddclass(
									selector.split(" ")[0].split("#")[1],
									css.getClassname());
						}
					} else {
						if (selector.contains(".")) {
							findlableaddclass(selector.split("\\.")[1],
									css.getClassname());
						} else {
							findlableaddclass(selector.split("#")[1],
									css.getClassname());
						}
					}
				}
			}
		}

		private static void findlableaddclass(String lableid, String classname) {
			boolean flag = false;
			for (Label label : contentlabels) {
				if (label.getId().equals(lableid)) {
					for (Attr attr : label.getAttributes()) {
						if (attr.getName().toLowerCase().equals("class")) {
							attr.setValue(attr.getValue() + " " + classname);
							flag = true;
						}
					}
					if (!flag) {
						label.addAttr(new Attr("class", classname));
					}
					flag = false;
				}
			}
		}

		private static void writeprocess() {
			replaceOldLabel();
			writepfiles();
			writecssfile();
		}

		private static void gennewClass() {
			int count = 1;
			boolean flag = false;
			boolean hastd = false;
			for (CssBlock css : cssBlocks) {
				String classname = dirname + count;
				hastd = false;
				for (String selector : css.getSelector()) {
					String id = selector.split("#")[1];// wrong! when table#xxx
														// td
					if (id.trim().contains(" ")) {
						id = id.split(" ")[0];
						hastd = true;
					}
					for (Label label : contentlabels) {
						if (label.getId().equals(id)) {
							flag = false;
							for (Attr attr : label.getAttributes()) {
								if (attr.name.equals("class")) {
									attr.setValue(attr.getValue() + " "
											+ classname);
									flag = true;
									break;
								}
							}
							if (!flag) {
								label.addAttr(new Attr("class", classname));
							}
							break;
						}
					}
				}
				if (hastd) {
					css.setClassSelector("." + classname + " td");
				} else {
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
				reader = new InputStreamReader(new FileInputStream(pfile));
				while ((tempint = reader.read()) != -1) {
					content.append((char) tempint);
				}
				reader.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("error when reading html files");
			}
			contentStr = content.toString();
		}

		private static void pulloutCss() {

			createLabels(content, "<table");
			createLabels(content, "<td");
			myfiles.add(new MyFile(pfile, content.toString()));
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

		private static void writepfiles() {
			for (MyFile file : myfiles) {

				try {
					file.getpfile().createNewFile();
					Writer htmlwriter = new OutputStreamWriter(
							new FileOutputStream(file.getpfile()));
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
				writer = new OutputStreamWriter(new FileOutputStream(new File(
						"D:/html/pcss.css")));
				for (CssBlock css : cssBlocksInFile) {
					boolean flag = false;
					for (int i = 0; i < css.getSelector().size(); i++) {
						String str = css.getSelector().get(i);
						if (str.contains(" td")) {
							css.getSelector().remove(i);
							flag = true;
							break;
						}
					}
					String temp = "";
					if (flag) {
						temp = "." + css.getClassname() + ",\n."
								+ css.getClassname() + " td";
					} else {
						temp = "." + css.getClassname();
					}

					writer.append(temp + "{\n");
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
				replaceStr = replaceStr
						.concat(" id=" + ec + label.getId() + ec);
				for (int j = 0; j < label.getAttributes().size(); j++) {
					Attr tempattr = label.getAttributes().get(j);
					replaceStr = replaceStr.concat(" " + tempattr.getName()
							+ "=" + ec + tempattr.getValue() + ec);
				}

				for (MyFile mf : myfiles) {
					String str = mf.getContentStr();
					if (str.contains(label.getPid())) {
						mf.setContentStr(str.replace(label.getPid(), replaceStr));
						break;
					}
				}
			}
		}

		private static void createLabels(StringBuffer content, String lablename) {
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

				if (hasLblAttr(content.substring(start, end))) {
					Label tempLabel = new Label(content.substring(start, end),
							prefix);
					contentlabels.add(tempLabel);
					content.replace(start, end, tempLabel.getPid());
				}
				indexl = content.indexOf(lablename.toLowerCase());
				indexh = content.indexOf(lablename.toUpperCase());
			}
		}

		private static boolean hasLblAttr(String labelcontent) {
			// TODO Auto-generated method stub
			StringBuffer tempsb = new StringBuffer();
			tempsb.append(labelcontent.trim());
			int start = 0;
			int end = -1;
			start = tempsb.indexOf("<");
			for (int i = start + 1; i < tempsb.length(); i++) {
				if (Character.isWhitespace(tempsb.charAt(i))) {
					return true;
				}
			}
			return false;
		}
	}

	private static class MyFile {
		private File pfile;
		private String contentStr;

		public MyFile(File file, String str) {
			this.pfile = file;
			this.contentStr = str;
		}

		public File getpfile() {
			return pfile;
		}

		public String getContentStr() {
			return contentStr;
		}

		public void setContentStr(String contentStr) {
			this.contentStr = contentStr;
		}

	}

	private static class CssBlock {
		private ArrayList<String> selector = new ArrayList<String>();
		private Map<String, String> attributes = new HashMap<String, String>();
		private String classname;

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

		public void setClassSelector(String selector) {
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

		public void setClassname(String classname) {
			this.classname = classname;
		}

		public String getClassname() {
			return classname;
		}

	}

	private static class Label {
		private static int tablecount = 1;
		private static int tdcount = 1;
		private String pid;
		private String id;
		private String name;
		private ArrayList<Attr> attributes = new ArrayList<cssToolCombine.Attr>();
		private String escapechar = null;

		public Label(String str, String prefix) {
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
				genAttributes(tempsb.substring(end + 1).toString().trim());
				this.name = tempsb.substring(start + 1, end).trim()
						.toLowerCase();
				if (hasid()) {
					this.pid = "<" + this.getId() + "#";
				} else {
					if (this.name.equals("table")) {
						this.pid = "<" + prefix + this.name + tablecount + "!#";
						this.setId("");
						tablecount++;
					} else {
						this.pid = "<" + prefix + this.name + tdcount + "!#";
						this.setId("");
						tdcount++;
					}
				}
			} else {
				System.out.println("error in create lable with no attributes");
			}
		}

		private boolean hasid() {
			if (this.id == null || this.id.isEmpty()) {
				return false;
			} else {
				return true;
			}
		}

		private void genAttributes(String str) {
			String name = "";
			String value = "";
			String rawvalue = "";
			str = str.replace("\t", " ");
			int offset = 0;
			int start = 0;
			int end = str.indexOf("=");
			while (end != str.length()) {
				if (end == -1) {
					System.out.println("wrong in gen attr");
					break;
				}
				name = str.substring(start, end).trim();
				start = end;
				end = str.indexOf("=", end + 1);
				if (end == -1) {
					end = str.length();
				}
				if (escapechar == null) {
					switch (str.charAt(start + 1)) {
					case '\'':
						this.setEscapechar("\'");
						break;
					case '\"':
						this.setEscapechar("\"");
						break;
					case '~':
						if (str.charAt(start + 2) == '\"') {
							this.setEscapechar("~\"");
						} else {
							this.setEscapechar("");
						}
						break;
					default:
						this.setEscapechar("");
					}
				}
				rawvalue = str.substring(start + 1 + escapechar.length(), end);
				value = rawvalue.substring(0, rawvalue.lastIndexOf(escapechar)).trim();
				offset = rawvalue.length() - 1
						- rawvalue.lastIndexOf(escapechar)
						- escapechar.length();
				start = end - offset;
				if (name.toLowerCase().equals("id")) {
					this.setId(value);
				}
				else if (name.toLowerCase().equals("class")) {
					if(!value.contains(" ")){
						addAttr(new Attr(name, value));
					}else if(!value.contains("\"") && !value.contains("\'") && !value.contains("`")){
						ArrayList<String> alvalue = new ArrayList<String>();
						for(String tempstr : value.split(" ")){
							alvalue.add(tempstr);
						}
						addAttr(new Attr(name,alvalue));
					}else{
						if(value.contains("\"")){
							genClassValue(value,"\"");
						}else if(value.contains("\'")){
							genClassValue(value,"\'");
						}else{
							genClassValue(value,"`");
						}
					}
				} else {
					addAttr(new Attr(name, value));
				}
			}
		}
		
		private void genClassValue(String value,String es){
			int cstart = 0;
			int cend = 0;
			ArrayList<String> values = new ArrayList<String>();
			StringBuffer valuesb = new StringBuffer();
			valuesb = new StringBuffer(value);
			cstart = valuesb.indexOf(es);
			cend = valuesb.lastIndexOf(es);
			values.add(valuesb.substring(cstart, cend + 1));
			valuesb.replace(cstart, cend + 1, "");
			for(String tempstr : valuesb.toString().split(" ")){
				if(!tempstr.isEmpty()){
					values.add(tempstr);									
				}
			}
			addAttr(new Attr(name,values));
		}

		public void addAttr(Attr attr) {
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
		private String name = "";
		private ArrayList<String> values = new ArrayList<String>();

		public Attr(String name, String value) {
			this.name = name;
			this.values.clear();
			this.values.add(value);
		}
		
		public Attr(String name, ArrayList<String> values) {
			this.name = name;
			this.values = values;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			String temp = "";
			for (String str : values) {
				temp += str + " ";
			}
			return temp.trim();
		}

		public void setValue(String value) {
			this.values.clear();
			this.values.add(value);
		}

		public void setValue(ArrayList<String> values) {
			this.values = values;
		}

		public void addValue(String value) {
			this.values.add(value);
		}
	}
}

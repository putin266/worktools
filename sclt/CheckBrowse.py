import xml.etree.ElementTree as Et
import xml.dom.minidom

tree = Et.parse('/Users/Ethan/QAD/src/service/bl/data/progress/xml/qadadm/browses.xml')

root = tree.getroot()
browselist = []

for child in root:
    name = ''
    code = ''
    for child2 in child:
        if child.tag == "brw_mstr":
            if child2.tag == 'brw_name':
                name = child2.text
            if child2.tag == 'brw_desc':
                code = child2.text
    if name != '' and code != '':
        browselist.append({'name': name, 'code': code})

for item in browselist:
    print(item)

tree = Et.parse('/Users/Ethan/QAD/src/service/bl/data/secureresource/browseresources.xml')
root = tree.getroot()
browselist2 = []

for child in root:
    for child2 in child:
        if "urn:browse:mfg:" in child2.text:
            browselist2.append(child2.text[15:])
        break

for item in browselist:
    if item['name'] not in browselist2:
        print(item)
        sub = Et.SubElement(root, 'SecureResource', {})
        suburi = Et.SubElement(sub, 'ResourceURI', {})
        suburi.text = "urn:browse:mfg:" + item['name']
        Et.SubElement(sub, 'ParentResourceURI', {})
        subismenu = Et.SubElement(sub, 'IsMenuEligible', {})
        subismenu.text = 'false'
        substringcode = Et.SubElement(sub, 'StringCode', {})
        substringcode.text = item['code']

tree.write("/Users/Ethan/QAD/temp/temp.xml")

xml = xml.dom.minidom.parse("/Users/Ethan/QAD/temp/temp.xml")
pretty_xml_as_string = xml.toprettyxml()

f = open("/Users/Ethan/QAD/temp/temp.xml", 'w')
f.write(pretty_xml_as_string)
f.close()
f2 = open("/Users/Ethan/QAD/temp/temp2.xml", 'w')
with open("/Users/Ethan/QAD/temp/temp.xml", 'r') as file:
    for line in file:
        if not line.isspace():
            f2.write(line)

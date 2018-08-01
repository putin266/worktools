from xml.etree.ElementTree import Element, SubElement, tostring
import os

mapLocal = Element('mapLocal')
toolEnabled = SubElement(mapLocal, 'toolEnabled')
toolEnabled.text = 'true'
mappings = SubElement(mapLocal, 'mappings')

webuiPath = '/Users/Ethan/QAD/src/service/webui/erp-service-webui'
resourceRelativePath = '/src/main/resources/META-INF/resources/resources/'

for root, dirs, files in os.walk(webuiPath + resourceRelativePath):
    for file in files:
        path = os.path.join(root, file)
        string = path.split('META-INF/resources/')[1]
        mapLocalMapping = SubElement(mappings, 'mapLocalMapping')
        sourceLocation = SubElement(mapLocalMapping, 'sourceLocation')
        protocol = SubElement(sourceLocation, 'protocol')
        protocol.text = 'http'
        host = SubElement(sourceLocation, 'host')
        host.text = 'blackcase.qad.com'
        port = SubElement(sourceLocation, 'port')
        port.text = '22010'
        pathtag = SubElement(sourceLocation, 'path')
        pathtag.text = '/qad-central/' + string
        dest = SubElement(mapLocalMapping, 'dest')
        dest.text = path
        enabled = SubElement(mapLocalMapping, 'enabled')
        enabled = 'true'
        caseSensitive = SubElement(mapLocalMapping, 'caseSensitive')
        caseSensitive.text = 'false'

targetFile = open('/Users/Ethan/temp/charles.xml', 'w')

targetFile.write("<?xml version='1.0' encoding='UTF-8' ?>\n<?charles serialisation-version='2.0' ?>\n")
targetFile.write(tostring(mapLocal).decode('utf-8'))


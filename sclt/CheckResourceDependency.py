import os
import pprint
import xml.etree.ElementTree as ET
import xml.dom.minidom as Minidom

pp = pprint.PrettyPrinter(indent=4)


class FileDependency:
    def __init__(self, filename, arg_external_dependency_files):

        self.class_name = ""
        self.external_dependency_files = dict()
        self.service_locators = list()
        self.all_imports = list()
        self.raw_dependencies = list()
        self.property_dependencies = dict()
        self.confirmed_dependencies = dict()
        self.method_dependencies = dict()

        if not os.path.isfile(filename):
            return

        with open(filename, "r", encoding="utf-8") as f:

            is_import_clause_exist = True
            is_property_clause_exist = True
            is_class_clause_exist = True

            lines = f.readlines()
            for line in lines:
                line = line.strip(' ')
                # get all 'using' import
                if is_import_clause_exist and line.startswith("using"):
                    self.handle_import_line(line)

                if is_class_clause_exist and line.startswith("class"):
                    full_name = line[line.find("com"):-2]
                    end_index = full_name.find(" ")
                    start_index = full_name.rfind(".", 0, end_index) + 1
                    self.class_name = full_name[start_index:end_index]
                    self.check_imported_external_file(arg_external_dependency_files)
                    is_import_clause_exist = False
                    is_class_clause_exist = False

                if is_property_clause_exist and line.startswith("define") and line.find("property") != -1:
                    self.handle_property_line(line)

                if is_property_clause_exist and line.startswith("method"):
                    is_property_clause_exist = False

                if not is_property_clause_exist:
                    if line.startswith("method"):
                        tokens = line.split(' ')
                        if tokens[2] == "static":
                            current_method = tokens[4].split('(')[0]
                        else:
                            current_method = tokens[3].split('(')[0]
                    else:
                        self.handle_method_line(line, current_method)

    @staticmethod
    def isvalid_resource_name(entity_name):
        if entity_name.startswith("I") and not entity_name.endswith("DataObject"):
            if entity_name[1:2].isupper():
                return True
        return False

    @staticmethod
    def isvalid_service_locator_name(entity_name):
        return entity_name.endswith("Services")

    @staticmethod
    def get_related_service_locator(full_name):
        module_name = full_name.split('.')[2]
        module_name = module_name[0].capitalize() + module_name[1:]
        return module_name + "Services"

    @staticmethod
    def get_resource_uri(full_name):
        return "urn:be:" + full_name

    def check_imported_external_file(self, arg_external_dependency_files):
        file_names = arg_external_dependency_files.keys()
        for entity_name in self.all_imports:
            if entity_name in file_names \
              and entity_name not in self.external_dependency_files.keys():
                self.external_dependency_files[entity_name] = \
                    arg_external_dependency_files[entity_name]

    def handle_import_line(self, line):
        if line.find("com.qad") == -1:
            return
        full_name = line[line.find("com"):-2]
        entity_name = full_name[full_name.rindex(".") + 1:]
        self.all_imports.append(entity_name)
        if self.isvalid_resource_name(entity_name):
            raw_dependency = dict()
            raw_dependency["full_name"] = full_name
            raw_dependency["entity_name"] = entity_name
            raw_dependency["service_locator"] = self.get_related_service_locator(full_name)
            # TODO: Default values Depend on coding standard
            raw_dependency["service_name"] = entity_name[1:] + "Service"
            raw_dependency["service_call"] = raw_dependency["service_name"] + ":"
            raw_dependency["instantiate_clause"] = \
                raw_dependency["service_locator"] + ":Get" + entity_name[1:]
            raw_dependency["resource_uri"] = self.get_resource_uri(raw_dependency["full_name"])
            self.raw_dependencies.append(raw_dependency)

        elif self.isvalid_service_locator_name(entity_name):
            self.service_locators.append(entity_name)

    def handle_property_line(self, line):
        tokens = line.split(" ")
        if tokens.__len__() >= 6:
            for raw_dependency in self.raw_dependencies:
                if tokens[5].__eq__(raw_dependency["entity_name"]) or \
                  (tokens[5].__eq__("class") and tokens[6].__eq__(raw_dependency["entity_name"])):
                    raw_dependency["service_name"] = tokens[3]
                    raw_dependency["service_call"] = tokens[3] + ":"
                    self.property_dependencies[raw_dependency["service_name"]] = raw_dependency
                    # print("property:", raw_dependency["entity_name"], raw_dependency["service_name"])

    def handle_method_line(self, line, current_method):
        if current_method not in self.method_dependencies.keys():
            self.method_dependencies[current_method] = dict()

        tmp_confirmed_dependencies = dict()
        for raw_dependency in self.raw_dependencies:
            # instantiate_clause exits means the entity is instantiated in this file
            if line.find(raw_dependency["service_call"]) != -1 or line.find(raw_dependency["instantiate_clause"]) != -1:
                tmp_confirmed_dependencies[raw_dependency["full_name"]] = raw_dependency
                # print(current_method, "raw", raw_dependency)

        external_class_names = self.external_dependency_files.keys()
        for class_name in external_class_names:
            external_dependency_file = self.external_dependency_files[class_name]
            methods = external_dependency_file.method_dependencies.keys()
            for method_name in methods:
                if line.find(method_name) != -1:
                    # TODO: May not fit every external function call, only when class_name is default
                    if line.find(class_name + ":" + method_name) != -1:
                        # print(current_method, "external method", class_name + ":" + method_name)
                        # pp.pprint(external_dependency_file.method_dependencies[method_name])
                        dependency_names = external_dependency_file.method_dependencies[method_name].keys()
                        for dependency_name in dependency_names:
                            tmp_confirmed_dependencies[dependency_name] = \
                                external_dependency_file.method_dependencies[method_name][dependency_name]

            properties = external_dependency_file.property_dependencies.keys()
            for property_name in properties:
                if line.find(property_name) != -1:
                    # TODO: May not fit every external property call, only when class_name is default
                    if line.find(class_name + ":" + property_name) != -1:
                        # print(current_method, "external property", class_name + ":" + property_name)
                        # pp.pprint(external_dependency_file.property_dependencies[property_name])
                        dependency = external_dependency_file.property_dependencies[property_name]
                        tmp_confirmed_dependencies[dependency["full_name"]] = dependency

        dependency_names = tmp_confirmed_dependencies.keys()
        for dependency_name in dependency_names:
            dependency = tmp_confirmed_dependencies[dependency_name]
            self.method_dependencies[current_method][dependency["full_name"]] = dependency
            if dependency_name not in self.confirmed_dependencies.keys():
                # print("confirmed dependency:", dependency["entity_name"], current_method)
                # dependency['first_calling_method'] = current_method
                self.confirmed_dependencies[dependency_name] = dependency

    def write(self):
        pp.pprint("========================================================================================")
        pp.pprint(self.class_name)
        pp.pprint("========================================================================================")
        print("All dependencies")
        pp.pprint(self.confirmed_dependencies)
        pp.pprint("========================================================================================")
        print("Method dependencies")
        pp.pprint(self.method_dependencies)
        pp.pprint("========================================================================================")
        print("Property dependencies")
        pp.pprint(self.property_dependencies)
        pp.pprint("========================================================================================")
        print("Confirmed dependencies")
        pp.pprint(self.confirmed_dependencies)
        pp.pprint("========================================================================================")
        pp.pprint("========================================================================================")


def beautify_xml(xml_name):
    xml = Minidom.parse(xml_name)
    pretty_xml_as_string = xml.toprettyxml()

    f = open(xml_name, 'w')
    f.write(pretty_xml_as_string)
    f.close()


def dump_to_xml(xml_name, business_entities):
    root = ET.Element("ResourceDependencies", {"xmlns:xsi":"http://www.w3.org/2001/XMLSchema-instance"})
    tree = ET.ElementTree(root)
    source_urn_names = business_entities.keys()
    for source_urn_name in source_urn_names:
        merged_dependencies = dict()
        for dependency_file in business_entities[source_urn_name]:
            dependency_names = dependency_file.confirmed_dependencies.keys()
            for dependency_name in dependency_names:
                if dependency_name not in merged_dependencies.keys():
                    merged_dependencies[dependency_name] = dependency_file.confirmed_dependencies[dependency_name]
        merged_dependency_names = merged_dependencies.keys()
        for merged_dependency_name in merged_dependency_names:
                resource_mapping_element = ET.SubElement(root, "ResourceDependency")
                source_uri_element = ET.SubElement(resource_mapping_element, "ResourceURI")
                source_uri_element.text = source_urn_name
                dependency_uri_element = ET.SubElement(resource_mapping_element, "DependencyURI")
                dependency_uri_element.text = merged_dependencies[merged_dependency_name]["resource_uri"]

    tree.write(xml_name)
    beautify_xml(xml_name)


def isfile(dir_object):
    try:
        os.listdir(dir_object)  # tries to get the objects inside of this object
        return False  # if it worked, it's a folder
    except Exception:  # if not, it's a file
        return True


def isvalid_entity_interface_name(filename):
    return filename.startswith("I") and filename.endswith(".cls") and not filename.endswith("DataObject.cls")


def is_file_belong_to_interface(filename, interface_name):
    entity_name = interface_name[1:]
    if filename.startswith(entity_name):
        if filename == entity_name + ".cls" \
          or filename == entity_name + "BS.cls" \
          or filename == entity_name + "BE.cls" \
          or filename == entity_name + "DA.cls" \
          or filename == entity_name + "DataObject.cls":
            return True
    return False


def list_files_recursively(base_dir, dir_suffix, entity_api_files):
    dir_suffix = dir_suffix + "/"
    dir_objects = os.listdir(base_dir + dir_suffix)  # tries to get the objects inside of this object
    entity_api_files[dir_suffix] = list()

    for dir_object in dir_objects:  # check if very object in the folder ...
        if isfile(base_dir + dir_suffix + dir_object):  # ... is a file.
            if isvalid_entity_interface_name(dir_object):
                entity_api_files[dir_suffix].append(dir_object[0:-4])  # remove ".cls"
        else:
            list_files_recursively(base_dir, dir_suffix + dir_object, entity_api_files)


def get_namespace(suffix, interface_name):
    return suffix.replace("/", ".") + interface_name


def list_entity_files(module_dir):

    api_dir = module_dir + "src/api"
    impl_dir = module_dir + "src/impl"

    entity_api_files = dict()
    entity_impl_files = dict()
    list_files_recursively(api_dir, "", entity_api_files)

    suffixes = entity_api_files.keys()
    for suffix in suffixes:
        dir_objects = os.listdir(impl_dir + suffix)
        for interface in entity_api_files[suffix]:
            resource_uri = FileDependency.get_resource_uri(get_namespace(suffix, interface))
            entity_impl_files[resource_uri] = list()
        for dir_object in dir_objects:
            if isfile(impl_dir + suffix + dir_object):
                for interface in entity_api_files[suffix]:
                    if is_file_belong_to_interface(dir_object, interface):
                        resource_uri = FileDependency.get_resource_uri(get_namespace(suffix, interface))
                        entity_impl_files[resource_uri].append(impl_dir + suffix + dir_object)
                        break

    return entity_impl_files


def main():
    # configurations
    module_dir = "/Users/daniel/Documents/projects/qad/qadmodule_repository/service/bl/trunk/"
    entity_files = list_entity_files(module_dir)
    pp.pprint(entity_files)

    external_file_dict = dict()
    external_file_name_list = list()
    # TODO: maintain your util files here
    external_file_name_list.append(module_dir + "/src/impl/com/qad/service/DependentServices.cls")
    external_file_name_list.append(module_dir + "/src/impl/com/qad/service/DOCache.cls")
    external_file_name_list.append(module_dir + "/src/impl/com/qad/service/ServiceUtilities.cls")

    for filename in external_file_name_list:
        external_dependency = FileDependency(filename, external_file_dict)
        external_file_dict[external_dependency.class_name] = external_dependency
        # external_dependency.write()

    business_entities = dict()

    resource_uris = entity_files.keys()
    for resource_uri in resource_uris:
        business_entities[resource_uri] = list()
        for filename in entity_files[resource_uri]:
            print("processing", filename, "...")
            file_dependency = FileDependency(filename, external_file_dict)
            # file_dependency.write()
            business_entities[resource_uri].append(file_dependency)
            print("processed", filename, "!")

    dump_to_xml(module_dir + "/tmp.xml", business_entities)
main()

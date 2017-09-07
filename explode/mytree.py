import treelib as tl
import itertools as it
import os
import io
import subprocess
from datetime import date


class Tree(tl.Tree):
    def __init__(self, filename, working_dir='~', start_date='', end_date='', log_dir=''):
        super(Tree, self).__init__()
        self.files = []
        if start_date == '':
            self.start_date = '{' + date.today().isoformat() + '}'
        else:
            self.start_date = '{' + start_date + '}'
        if end_date == '':
            self.end_date = '{' + date.today().isoformat() + '}'
        else:
            self.end_date = '{' + end_date + '}'
        self.working_dir = working_dir
        self.import_tree(filename)
        self.log_dir = log_dir

    def filter_by_file(self, file_list):
        def func(n):
            if n.tag in file_list:
                return True
            else:
                return False
        node_list = []
        for node in self.filter_nodes(func=func):
            node_list.append(node.identifier)
        for identifier in node_list:
            if self.get_node(identifier) is not None:
                self.remove_node(identifier)
        self._get_file_list()

    def filter_by_desc(self):

        def func(n):
            if "*Circularreference*" in n.data.description:
                return True
            elif "*Duplicatereference*" in n.data.description:
                return True
            elif "*Recursivecall*" in n.data.description:
                return True
            elif "flh_mstr.d" in n.data.description:
                return True
            elif "drl_mstr.d" in n.data.description:
                return True
            else:
                return False
        node_list = []
        for node in self.filter_nodes(func=func):
            node_list.append(node.identifier)
        for identifier in node_list:
            if self.get_node(identifier) is not None:
                self.remove_node(identifier)
        self._get_file_list()

    def _get_file_list(self):
        file_list = []
        for node in self.all_nodes_itr():
            if not (node.data.filename in file_list):
                file_list.append(node.data.filename)
        self.files = []
        for filename in file_list:
            self.files.append(FileNode(filename))

    def get_file_list(self):
        file_list = []
        for file in self.files:
            file_list.append(file.filename)
        return file_list

    def import_tree(self, filename):
        f = open(filename, encoding="UTF8")
        current_depth = 0
        line_num = 0
        parent_nodes = []
        for line in f:
            line_num = line_num + 1
            l = Line(line, line_num)
            offset = l.depth - current_depth
            if current_depth == 0:
                self.create_node(l.filename, l.line_num, data=l)
                parent_nodes.append(l.line_num)
                current_depth = current_depth + 1
            else:
                if offset > 0:
                    current_depth = current_depth + 1
                    parent_nodes.append(l.line_num - 1)
                elif offset < 0:
                    current_depth = current_depth + offset
                    for _ in it.repeat(None, abs(offset)):
                        parent_nodes.pop()
                self.create_node(l.filename, l.line_num, parent_nodes[-1], l)
        self._get_file_list()

    def get_svn_info(self):
        self._get_file_full_path()
        for file in self.files:
            self._get_svn_log(file)

    def _get_svn_log(self, file):
        if self.log_dir == '':
            if file.path != '':
                cp = subprocess.run(['svn', 'log', file.path, '-r', self.start_date + ':' + self.end_date],
                                    cwd=self.working_dir,
                                    stdout=subprocess.PIPE)
                log = cp.stdout.decode('UTF8')
                f = open('logs/' + file.filename + '.log', 'w')
                f.write(log.decode('UTF8'))
                f.close()
            else:
                log = ''
        else:
            if file.path != '':
                for root, dirs, files in os.walk(self.log_dir):
                    for _file in files:
                        if _file == file.filename + '.log':
                            path = os.path.join(root, _file)
                            break
                f = open(path, 'r')
                lines = []
                for line in f:
                    lines.append(line)
                f.close()
                log = ''.join(lines)
            else:
                log = ''
        log_io = io.StringIO(log)
        go_next_block = True
        for line in log_io:
            is_content = False
            is_line = False
            for c in line:
                if not is_line and c == '-':
                    is_line = True
                if c != '-' and c != '\n':
                    is_line = False
                if c != '-' and c != '\n' and c != ' ':
                    is_content = True
                    break
            if go_next_block and is_line:
                go_next_block = False
            elif go_next_block and not is_line:
                continue
            elif not go_next_block and is_line:
                go_next_block = True
            if is_content:
                if not line.startswith('r'):
                    ticket_number = []
                    is_second_part = False
                    for char in line:
                        if not is_second_part:
                            ticket_number.append(char)
                            if char == '-':
                                is_second_part = True
                        else:
                            try:
                                int(char)
                                ticket_number.append(char)
                            except ValueError:
                                go_next_block = True
                                break
                    ticket = ''.join(ticket_number)
                    file.add_ticket(ticket)
                    print(file.filename, ticket)
        log_io.close()

    def save_to_file(self, ticket_list_filename='', file_ticket_list_filename=''):
        if ticket_list_filename == '':
            ticket_list_filename = 'ticket_list.txt'
        if file_ticket_list_filename == '':
            file_ticket_list_filename = 'file_ticket_list.txt'
        f = open(ticket_list_filename, 'w')
        f.write('\n'.join(self.get_ticket_list()))
        f.close()
        f = open(file_ticket_list_filename, 'w')
        for file in self.files:
            f.write(file.to_string())
        f.close()

    def _get_file_full_path(self):
        file_list = self.get_file_list()
        for root, dirs, files in os.walk(self.working_dir):
            for file in files:
                if file in file_list:
                    path = os.path.join(root, file)
                    for f in self.files:
                        if f.filename == file:
                            f.path = path
                            break

    def get_ticket_list(self):
        ticket_list = []
        for file in self.files:
            for ticket in file.tickets:
                if ticket not in ticket_list:
                    ticket_list.append(ticket)
        return ticket_list


class Line(object):
    def __init__(self, line, line_num):
        self._format(line)
        self.line_num = line_num

    def _format(self, line=""):
        self.depth = 0
        index = -1
        first_char = 0
        for c in line:
            index = index + 1
            if c == ".":
                self.depth = self.depth + 1
            elif c == " ":
                first_char = index + 1
            else:
                break
        line = line[first_char:]
        str_list = line.split("\n")[0].split(" ")
        self.filename = str_list[0]
        if len(str_list) > 1:
            self.description = "".join(str_list[1:])
        else:
            self.description = ""


class FileNode(object):
    def __init__(self, filename):
        self.filename = filename
        self.path = ''
        self.tickets = []

    def to_string(self):
        return self.filename + ',' + ';'.join(self.tickets) + '\n'

    def add_ticket(self, ticket):
        self.tickets.append(ticket)






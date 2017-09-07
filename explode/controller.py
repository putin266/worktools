from mytree import Tree
import subprocess
import io


def fun1():
    cp = subprocess.run(['svn', 'log', 'mf1a.p', '-r', '{2016-01-26}:{2017-07-20}'],
                        cwd='/Users/Ethan/QAD/src/erp_trunk/src/us',
                        stdout=subprocess.PIPE)
    p = cp.stdout
    print(p)

    sio = io.StringIO(p.decode('UTF8'))
    for line in sio:
        is_content = False
        for c in line:
            if c != '-' and c != '\n' and c != ' ':
                is_content = True
                break
        if is_content:
            print('is content')
        print(line, end="")

def fun2():
    f = open('ticket_list.txt', 'w')
    x = 'hell'
    f.write(x)
    f.close()

def tree1():
    tree = Tree("fscarmt.e", '/Users/Ethan/QAD/src/erp_trunk/src', start_date='2016-01-26', end_date='2017-07-20',
                log_dir='/Users/Ethan/PycharmProjects/explod/logs')
    tree.filter_by_file(["applhelp.p", "pxmsg.i", "pxmsglib.p"])
    tree.get_svn_info()
    tree.save_to_file()

tree1()

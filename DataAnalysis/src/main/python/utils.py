import sys

def warn(message):
    sys.stderr.write(message + '\n')


def tee(path, mode='w'):
    sys.stdout = Tee(path, mode)

class Tee(object):
    def __init__(self, path, mode):
        self.terminal = sys.stdout
        self.log = open(path, mode)

    def write(self, message):
        self.terminal.write(message)
        self.log.write(message)
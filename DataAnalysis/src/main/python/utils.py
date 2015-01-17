import sys

DAT = '../../../dat'
RES = '../../../results'

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


def mean(l):
    return 1.0 * sum(l) / len(l)

def longest_common_substring(s1, s2):
    """
    From http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Longest_common_substring#Python2
    :param s1: string 1
    :param s2: string 2
    :return: length of the longest contiguous substring they both contain (or 0)
    """
    m = [[0] * (1 + len(s2)) for i in xrange(1 + len(s1))]
    longest, x_longest = 0, 0
    for x in xrange(1, 1 + len(s1)):
        for y in xrange(1, 1 + len(s2)):
            if s1[x - 1] == s2[y - 1]:
                m[x][y] = m[x - 1][y - 1] + 1
                if m[x][y] > longest:
                    longest = m[x][y]
                    x_longest = x
            else:
                m[x][y] = 0
    return len(s1[x_longest - longest: x_longest])
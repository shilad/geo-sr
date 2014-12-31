import csv

from utils import *

PERSONALIZED_FIELDS = [
    'person',
    'familiarity1',
    'familiarity2',
    'valence1',
    'valence2',
    'relatedness'
]

INT_FIELDS = [
    'familiarity1',
    'familiarity2',
    'valence1',
    'valence2',
    'relatedness',
    'popRank1',
    'popRank2',
]

FLOAT_FIELDS = [
    'spherical',
    'geodetic',
    'countries',
    'states',
    'graph',
    'sr',
    'typeSr',
]

VALIDATION_PAIRS = [
    #('high', 'Harvard University', 'Princeton University'),
    ('high', 'Great Britain', 'United Kingdom'),
    ('low', 'Florida', 'Hong Kong'),
    ('low', 'Bermuda Triangle', 'Minnesota'),
]

class ResponseDb:
    def __init__(self):
        self.responses = self.read_raw()

    def read_raw(self):
        f = open('dat/pair-responses.tsv', 'r')
        responses = []
        for r in csv.DictReader(f, delimiter='\t'):
            responses.append(Response(r))
        f.close()
        warn('read %d responses' % (len(responses)))
        return responses



class Response:
    def __init__(self, row):
        for field, val in row.items():
            if val in ('', 'NaN', 'null'):
                val = None
            if val and field in FLOAT_FIELDS:
                val = float(val)
            if val and field in INT_FIELDS:
                val = int(round(float(val)))
            setattr(self, field, val)

    def get_location_key(self):
        return tuple(sorted([self.location1, self.location2]))

    def get_validation_code(self):
        for code, l1, l2 in VALIDATION_PAIRS:
            if self.get_location_key() == (l1, l2):
                return code
        return None

    def is_validation(self):
        return self.get_validation_code() is not None

    def has_response(self):
        return self.relatedness > 0

    def is_correct_validation(self):
        code = self.get_validation_code()
        if code == 'low':
            return self.relatedness not in (4, 5)
        elif code == 'high':
            return self.relatedness not in (1, 2)
        else:
            assert False


if __name__ == '__main__':
    db = ResponseDb()
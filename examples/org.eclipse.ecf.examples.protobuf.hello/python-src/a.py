'''
Created on Jan 29, 2018

@author: slewis
'''

from b import B

class A(object):
    
    def run(self):
        b = B()
        try: 
            b.run()
        except Exception as exc:
            raise Exception('exeception in class A') from exc
        
print("baz loaded")
from foo.bar.foobar import Foo

class Bar(Foo):
    def __init__(self):
        super().__init__()
        print("Bar.init")


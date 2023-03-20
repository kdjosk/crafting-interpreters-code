
class Boolean:
    def ifBlock(self, ifTrue, ifFalse):
        pass

class MyTrue(Boolean):
    def ifBlock(self, ifTrue, ifFalse):
        return ifTrue()


class MyFalse(Boolean):
    def ifBlock(self, ifTrue, ifFalse):
        return ifFalse()
        


if __name__ == "__main__":
    true = MyTrue()
    false = MyFalse()

    truth = {True: true, False: false}

    a = 3
    truth[a > 3].ifBlock(lambda: print("greater"), lambda: print("less or equal"))

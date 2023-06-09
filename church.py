def conditional(bool_val, true_func, false_func):
    return bool_val(true_func, false_func)()

# Define the Church boolean for true
true = lambda x, y: x

# Define the Church boolean for false
false = lambda x, y: y

# Define two functions to execute based on the boolean value
true_func = lambda: print("True branch")
false_func = lambda: print("False branch")

# Call the conditional function with the true Church boolean
conditional(true, true_func, false_func)  # Output: True branch

# Call the conditional function with the false Church boolean
conditional(false, true_func, false_func)  # Output: False branch


class A:
    def sayHi(self):
        print(f"Hi, Im {self.name}")


a = A()
a.name = " Żaneta"

b = A()
b.name = "Hieronim"

b.sayHi = a.sayHi

del a

b.sayHi()


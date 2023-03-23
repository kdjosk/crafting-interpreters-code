#include <stdio.h>
#include <stdbool.h>

int main(void) {
    {
        if (true) int a = 5;     
        printf("%d\n", a);
    }
    return 0;
}
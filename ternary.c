#include <stdio.h>
#include <stdbool.h>

int main(void) {
    int a = 1;
    {
        int a = a + 2;
        printf("%d\n", a);
    }
    return 0;
}
CC = arm-linux-androideabi-gcc
CFLAGS = -Wall -g
LDFLAGS = -llog
SRC = p.c
OBJ = $(SRC:.c=.o)
EXE = p
 
all: $(SRC) $(EXE)

$(EXE): $(OBJ)
	$(CC) -o $@ $^ $(LDFLAGS)
 
%.o: %.c
	$(CC) -o $@ -c $< $(CFLAGS)

clean:
	rm -f *.o $(EXE)

install:
	adb push p /data/local
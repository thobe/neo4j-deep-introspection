# Generic Makefile for JNI libraries for use by the ant jnic-task.

version=$(subst -SNAPSHOT,,$(VERSION))

JAVA_INCLUDES=-I"$(JDK_HOME)/include" \
              -I"$(JDK_HOME)/include/$(OS)"\
              -I"$(JDK_HOME)/Headers"

SOURCES=$(wildcard *.c)
LIBRARY=$(BUILDPATH)/$(LIBPFX)$(LIBNAME)$(JNISFX)
OBJECTS=$(SOURCES:%.c=$(BUILDPATH)/%.o)

# Defaults for GCC
LIBPFX=lib
LIBSFX=.so
ARSFX=.a
JNISFX=$(LIBSFX)
CC=gcc
LD=gcc
LIBS=
# Default to Sun recommendations for JNI compilation
#COPT=-O2 -fomit-frame-pointer
COPT=-fno-omit-frame-pointer -fno-strict-aliasing 
CASM=-S
ifeq ($(DEBUG),true)
CDEBUG=-g
endif
COUT=-o $@
CINCLUDES=$(JAVA_INCLUDES) -I"$(HEADERPATH)"
CDEFINES=-D_REENTRANT
PCFLAGS=-W -Wall -Wno-unused -Wno-parentheses
CFLAGS=$(PCFLAGS) $(CFLAGS_EXTRA) $(COPT) $(CDEBUG) $(CDEFINES) $(CINCLUDES)
LDFLAGS=-o $@ -shared 

### WINDOWS ###
ifeq ($(OS),win32)
ARCH=$(shell uname -m | sed 's/i.86/i386/g')
CDEFINES=-DHAVE_PROTECTION
WINDRES=windres
STRIP=@echo
LIBPFX=
LIBSFX=.dll

ifeq ($(CC),gcc)
CC += -mno-cygwin
LD += -mno-cygwin -Wl,--add-stdcall-alias
endif

ifeq ($(ARCH),amd64)

WINDRES=x86_64-pc-mingw32-windres

# Uncomment to enable MINGW64 cross compiler
# Last build attempt has too many runtime problems (alloca broken) (080831)
#MINGW = x86_64-pc-mingw32-gcc
ifneq ($(MINGW),)
CC = $(MINGW) -m64 -mno-cygwin
LD = $(CC)
LDFLAGS=-o $@ -shared
LIBS= -lmingwex -lkernel32 -lmsvcrt
FFI_CONFIG += --host=x86_64-pc-mingw32
else
# MSVC (wrapper scripts)
CC=$(FFI_SRC)/../cc.sh -m64
LD=$(FFI_SRC)/../ld.sh -m64
ARSFX=.lib
FFI_CONFIG += --host=x86_64-pc-mingw32 && rm -f include/ffitarget.h && cp $(FFI_SRC)/include/*.h $(FFI_SRC)/src/x86/ffitarget.h include
FFI_ENV += LD="$(LD)" CPP=cpp
endif
endif
endif

### LINUX ###
ifeq ($(OS),linux)
ARCH=$(shell uname -m | sed 's/i.86/i386/g')
PCFLAGS+=-fPIC
CDEFINES+=-DHAVE_PROTECTION
LDFLAGS+=-Wl,-soname,$@ 
endif

### FREEBDS ###
ifeq ($(OS),freebsd)
ARCH=$(shell uname -m | sed 's/i.86/i386/g')
PCFLAGS+=-fPIC
CINCLUDES+=-I/usr/X11R6/include
LDFLAGS=-o $@ -shared 
CDEFINES+=-DHAVE_PROTECTION -DFFI_MMAP_EXEC_WRIT
endif

### OPENBSD ###
ifeq ($(OS),openbsd)
ARCH=$(shell uname -m | sed 's/i.86/i386/g')
PCFLAGS+=-fPIC
CINCLUDES+=-I/usr/X11R6/include
LDFLAGS=-o $@ -shared 
CDEFINES+=-DHAVE_PROTECTION -DFFI_MMAP_EXEC_WRIT
endif

### SOLARIS ###
ifeq ($(OS),solaris)
ifeq ($(ARCH),)
ARCH=$(shell uname -p)
endif
PCFLAGS+=-fPIC
CDEFINES+=-DHAVE_PROTECTION -DFFI_MMAP_EXEC_WRIT
ifeq ($(ARCH), sparcv9)
  # alter CC instead of PCFLAGS, since we need to pass it down to libffi 
  # configure and some of the other settings in PCFLAGS might make the build 
  # choke
  CC += -m64  
  LD += -m64 
endif
endif

# Enable 64-bit builds if the arch demands it
ifeq ($(CC),gcc)
ifeq ($(ARCH),amd64)
  CC += -m64
  LD += -m64
endif
endif

### MAC OS X ###
ifeq ($(OS),darwin)
ARCH=$(shell arch)
ifeq ($(ARCH),ppc)
ALT_ARCHS=i386
else
ALT_ARCHS=ppc
endif
LIBSFX=.dylib
JNISFX=.jnilib
ifneq ($(SDKROOT),)
SYSLIBROOT=-Wl,-syslibroot,$(SDKROOT)
ISYSROOT=-isysroot $(SDKROOT)
ARCHFLAGS=-arch ppc -arch i386
ifneq ($(findstring 10.5,$(SDKROOT)),)
ALT_ARCHS+=x86_64 
ARCHFLAGS+=-arch x86_64
endif
endif
PCFLAGS+=$(ISYSROOT) -x objective-c
CDEFINES+=-DTARGET_RT_MAC_CFM=0 -DFFI_MMAP_EXEC_WRIT
LDFLAGS=$(ARCHFLAGS) -dynamiclib -o $@ -framework JavaVM \
  -compatibility_version $(shell echo ${version}|sed 's/^\([0-9][0-9]*\).*/\1/g') \
  -current_version $(version) \
  -mmacosx-version-min=10.3 \
  -install_name ${@F} \
  $(SYSLIBROOT)
# JAWT linkage handled by -framework JavaVM
LIBS=
endif

$(LIBRARY) : $(OBJECTS)
	$(LD) $(LDFLAGS) $(OBJECTS) $(LIBS)

$(BUILDPATH)/%.o : %.c
ifneq ($(SDKROOT),)
	$(CC) -arch $(ARCH) $(CFLAGS) -c $< -o $@.$(ARCH)
	for arch in $(ALT_ARCHS); do \
	  $(CC) -arch $$arch -I$(BUILDPATH)/libffi.$$arch/include $(CFLAGS) -c $< -o $@.$$arch; \
	done
	lipo -create -output $@ $@.*
else
	$(CC) $(CFLAGS) -c $< $(COUT)
endif

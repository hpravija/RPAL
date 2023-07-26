# Java compiler
JC = javac

# Directory structure
SRC_DIR = .
CSEM_DIR = csem
PARSER_DIR = parser
SCANNER_DIR = scanner

# Java source files
JAVA_FILES := $(wildcard $(SRC_DIR)/*.java) \
              $(wildcard $(CSEM_DIR)/*.java) \
              $(wildcard $(PARSER_DIR)/*.java) \
              $(wildcard $(SCANNER_DIR)/*.java)

# Output directory
OUTPUT_DIR = .

# Object files
OBJ_FILES := $(patsubst %.java, $(OUTPUT_DIR)/%.class, $(JAVA_FILES))

# Main target (default target)
all: $(OBJ_FILES)

# Compile Java files
$(OUTPUT_DIR)/%.class: %.java
	$(JC) -d $(OUTPUT_DIR) $<

# Clean build
clean:
	find . -name "*.class" -type f -delete

.PHONY: all clean
# Makefile for Whisper Model Downloads
# Provides easy orchestration of model downloads across all formats

.PHONY: help all pytorch gguf ct2 android summary clean test verify

# Configuration
SCRIPT_DIR := $(shell pwd)
MODELS_DIR := $(SCRIPT_DIR)/whisper_models
APP_PACKAGE := com.mira.com

# Default target
help:
	@echo "Whisper Model Download Makefile"
	@echo "==============================="
	@echo ""
	@echo "Available targets:"
	@echo "  all       Download all model formats (PyTorch, GGUF, CTranslate2)"
	@echo "  pytorch   Download only PyTorch (Transformers) models"
	@echo "  gguf      Download only GGUF (whisper.cpp) models"
	@echo "  ct2       Download only CTranslate2 (faster-whisper) models"
	@echo "  android   Deploy models to Android device"
	@echo "  summary   Generate download summary report"
	@echo "  test      Test model loading and verification"
	@echo "  verify    Verify downloaded models"
	@echo "  clean     Clean up downloaded models and temporary files"
	@echo ""
	@echo "Size-specific targets:"
	@echo "  tiny      Download only tiny models"
	@echo "  small     Download only small models"
	@echo "  medium    Download only medium models"
	@echo "  large     Download only large models"
	@echo ""
	@echo "Examples:"
	@echo "  make all              # Download everything"
	@echo "  make gguf             # Download only GGUF models"
	@echo "  make android          # Deploy to Android device"
	@echo "  make tiny             # Download only tiny models"
	@echo "  make clean && make all # Clean and re-download"

# Main targets
all: pytorch gguf ct2 android summary

pytorch:
	@echo "🐍 Downloading PyTorch (Transformers) models..."
	@./download_all_whisper_models.sh pytorch

gguf:
	@echo "🔧 Downloading GGUF (whisper.cpp) models..."
	@./download_all_whisper_models.sh gguf

ct2:
	@echo "⚡ Downloading CTranslate2 (faster-whisper) models..."
	@./download_all_whisper_models.sh ct2

android:
	@echo "📱 Deploying models to Android device..."
	@./download_all_whisper_models.sh android

summary:
	@echo "📊 Generating download summary..."
	@./download_all_whisper_models.sh summary

# Size-specific targets
tiny: gguf-tiny pytorch-tiny ct2-tiny

small: gguf-small pytorch-small ct2-small

medium: gguf-medium pytorch-medium ct2-medium

large: gguf-large pytorch-large ct2-large

# Individual format + size targets
gguf-tiny:
	@echo "🔧 Downloading tiny GGUF models..."
	@python3 scripts/download_gguf_models.py tiny

gguf-small:
	@echo "🔧 Downloading small GGUF models..."
	@python3 scripts/download_gguf_models.py small

gguf-medium:
	@echo "🔧 Downloading medium GGUF models..."
	@python3 scripts/download_gguf_models.py medium

gguf-large:
	@echo "🔧 Downloading large GGUF models..."
	@python3 scripts/download_gguf_models.py large

pytorch-tiny:
	@echo "🐍 Downloading tiny PyTorch models..."
	@python3 scripts/download_pytorch_models.py tiny

pytorch-small:
	@echo "🐍 Downloading small PyTorch models..."
	@python3 scripts/download_pytorch_models.py small

pytorch-medium:
	@echo "🐍 Downloading medium PyTorch models..."
	@python3 scripts/download_pytorch_models.py medium

pytorch-large:
	@echo "🐍 Downloading large PyTorch models..."
	@python3 scripts/download_pytorch_models.py large

ct2-tiny:
	@echo "⚡ Downloading tiny CTranslate2 models..."
	@python3 scripts/download_ct2_models.py tiny

ct2-small:
	@echo "⚡ Downloading small CTranslate2 models..."
	@python3 scripts/download_ct2_models.py small

ct2-medium:
	@echo "⚡ Downloading medium CTranslate2 models..."
	@python3 scripts/download_ct2_models.py medium

ct2-large:
	@echo "⚡ Downloading large CTranslate2 models..."
	@python3 scripts/download_ct2_models.py large

# Testing and verification
test: verify
	@echo "🧪 Testing model loading..."
	@echo "Testing GGUF models..."
	@find $(MODELS_DIR)/gguf -name "*.gguf" -exec echo "Testing {}" \; -exec hexdump -C -n 4 {} \; | head -20
	@echo ""
	@echo "Testing PyTorch models..."
	@find $(MODELS_DIR)/transformers -name "config.json" -exec echo "Found PyTorch model: {}" \;
	@echo ""
	@echo "Testing CTranslate2 models..."
	@find $(MODELS_DIR)/ct2 -name "config.json" -exec echo "Found CTranslate2 model: {}" \;

verify:
	@echo "🔍 Verifying downloaded models..."
	@echo ""
	@echo "GGUF Models:"
	@find $(MODELS_DIR)/gguf -name "*.gguf" 2>/dev/null | while read file; do \
		if [ -f "$$file" ]; then \
			size=$$(stat -f%z "$$file" 2>/dev/null || stat -c%s "$$file" 2>/dev/null); \
			size_mb=$$((size / 1024 / 1024)); \
			header=$$(hexdump -C -n 4 "$$file" | head -1 | awk '{print $$2 $$3 $$4 $$5}'); \
			if [ "$$header" = "47554746" ]; then \
				echo "✅ $$(basename $$file): $$size_mb MB (GGUF)"; \
			else \
				echo "❌ $$(basename $$file): Invalid header ($$header)"; \
			fi; \
		fi; \
	done
	@echo ""
	@echo "PyTorch Models:"
	@find $(MODELS_DIR)/transformers -name "config.json" 2>/dev/null | while read file; do \
		model_dir=$$(dirname "$$file"); \
		model_name=$$(basename "$$model_dir"); \
		size=$$(du -sh "$$model_dir" 2>/dev/null | cut -f1); \
		echo "✅ $$model_name: $$size"; \
	done
	@echo ""
	@echo "CTranslate2 Models:"
	@find $(MODELS_DIR)/ct2 -name "config.json" 2>/dev/null | while read file; do \
		model_dir=$$(dirname "$$file"); \
		model_name=$$(basename "$$model_dir"); \
		size=$$(du -sh "$$model_dir" 2>/dev/null | cut -f1); \
		echo "✅ $$model_name: $$size"; \
	done

# Cleanup
clean:
	@echo "🧹 Cleaning up downloaded models and temporary files..."
	@rm -rf $(MODELS_DIR)
	@rm -rf .venv
	@rm -rf __pycache__
	@rm -rf *.pyc
	@echo "✅ Cleanup complete"

# Quick deployment for Android testing
deploy-small:
	@echo "📱 Deploying small.en-q5_1.gguf to Android..."
	@if [ -f "$(MODELS_DIR)/gguf/ggml-small.en-q5_1.gguf" ]; then \
		adb shell "mkdir -p /storage/emulated/0/Android/data/$(APP_PACKAGE)/files/MiraWhisper/models"; \
		adb push "$(MODELS_DIR)/gguf/ggml-small.en-q5_1.gguf" "/storage/emulated/0/Android/data/$(APP_PACKAGE)/files/MiraWhisper/models/"; \
		adb shell run-as "$(APP_PACKAGE)" "mkdir -p files/models"; \
		adb shell run-as "$(APP_PACKAGE)" sh -c "cat > files/models/small.en-q5_1.gguf" < "$(MODELS_DIR)/gguf/ggml-small.en-q5_1.gguf"; \
		echo "✅ Deployed to both scoped and internal storage"; \
	else \
		echo "❌ Model not found. Run 'make gguf-small' first."; \
	fi

# Status check
status:
	@echo "📊 Model Download Status"
	@echo "======================="
	@echo ""
	@echo "Directory: $(MODELS_DIR)"
	@echo "Total size: $$(du -sh $(MODELS_DIR) 2>/dev/null | cut -f1 || echo 'Not found')"
	@echo ""
	@echo "GGUF models: $$(find $(MODELS_DIR)/gguf -name "*.gguf" 2>/dev/null | wc -l || echo 0)"
	@echo "PyTorch models: $$(find $(MODELS_DIR)/transformers -name "config.json" 2>/dev/null | wc -l || echo 0)"
	@echo "CTranslate2 models: $$(find $(MODELS_DIR)/ct2 -name "config.json" 2>/dev/null | wc -l || echo 0)"
	@echo ""
	@echo "Android deployment:"
	@if command -v adb >/dev/null 2>&1 && adb devices | grep -q "device$$"; then \
		echo "✅ Device connected"; \
		echo "Scoped storage: $$(adb shell "ls /storage/emulated/0/Android/data/$(APP_PACKAGE)/files/MiraWhisper/models/ 2>/dev/null | wc -l" || echo 0) files"; \
		echo "Internal storage: $$(adb shell run-as "$(APP_PACKAGE)" "ls files/models/ 2>/dev/null | wc -l" || echo 0) files"; \
	else \
		echo "❌ No Android device connected"; \
	fi
.PHONY: validate install-dev

install-dev:
	python3 -m pip install -r scripts/requirements.txt

validate:
	python3 scripts/validate.py

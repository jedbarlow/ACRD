all: acrd_manual.pdf

acrd_manual.pdf: manual.tex
	pdflatex --halt-on-error manual.tex &&\
	pdflatex --halt-on-error manual.tex &&\
	mv manual.pdf acrd_manual.pdf

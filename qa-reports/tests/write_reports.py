"""Compatibility entry point; current report evidence is mandatory."""
import runpy
from pathlib import Path
runpy.run_path(str(Path(__file__).with_name("update_release_reports.py")),run_name="__main__")

from setuptools import setup, find_packages

with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setup(
    name="quantum-jwt",
    version="0.1.0",
    author="Your Name",
    description="Quantum‑resistant JWT authentication framework",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/your-repo/quantum-auth-framework",
    packages=find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.9",
    install_requires=[
        "redis>=5.0.0",
    ],
)
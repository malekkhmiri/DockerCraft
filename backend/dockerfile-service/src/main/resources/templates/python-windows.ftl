FROM mcr.microsoft.com/windows/servercore:ltsc2019
SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop';"]
RUN Invoke-WebRequest -Uri "https://www.python.org/ftp/python/${version}.0/python-${version}.0-amd64.exe" -OutFile "python_installer.exe" ; \
    Start-Process python_installer.exe -ArgumentList '/quiet InstallAllUsers=1 PrependPath=1' -Wait ; \
    Remove-Item python_installer.exe
WORKDIR C:/app
COPY requirements.txt .
RUN python -m ensurepip ; pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["python", "main.py"]

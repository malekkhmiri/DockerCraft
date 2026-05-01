FROM python:${version}-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
<#if filesToCopy?has_content>
# Specific files to copy: ${filesToCopy?join(", ")}
</#if>
EXPOSE ${port}
CMD ["python", "main.py"]

from fastapi import APIRouter, UploadFile, File, HTTPException
from utils.xml_parser import parseVietnameseEInvoiceXML

router = APIRouter()

@router.post("/parse-xml")
async def parse_xml(file: UploadFile = File(...)):
    if not file.filename.endswith('.xml'):
        raise HTTPException(status_code=400, detail="Chỉ chấp nhận file định dạng .xml")
    
    try:
        content = await file.read()
        data = parseVietnameseEInvoiceXML(content)
        return data
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

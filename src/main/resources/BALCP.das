// DicomBrowser anonymization script
// by Kevin Archie, karchie@npg.wustl.edu
// deidentifies all attributes specified in the
// DICOM Basic Application Level Confidentiality Profile
//  (PS 3.15-2006, Annex E)
(0008,0014) := new UID		// Instance Creator UID
(0008,0018) := new UID		// SOP Instance UID
- (0008,0050)							// Accession Number
- (0008,0080)							// Institution Name
- (0008,0081)							// Institution Address
- (0008,0090)							// Referring Physician's Name
- (0008,0092)							// Referring Physician's Address
- (0008,0094)							// Referring Physician's Telephone Numbers
- (0008,1010)							// Station Name
- (0008,1030)							// Study Description
- (0008,103E)							// Series Description
- (0008,1040)							// Institutional Department Name
- (0008,1048)							// Physician(s) of Record
- (0008,1050)							// Performing Physician's Name
- (0008,1060)							// Name of Physician(s) Reading Study
- (0008,1070)							// Operator's Name
- (0008,1080)							// Admitting Diagnosis Description
- (0008,1155)							// Referenced SOP Instance UID
- (0008,2111)							// Derivation Description
- (0010,0010)							// Patient's Name
- (0010,0020)							// Patient ID
- (0010,0030)							// Patient's Birth Date
- (0010,0032)							// Patient's Birth Time
- (0010,0040)							// Patient's Sex
- (0010,1000)							// Other Patient IDs
- (0010,1001)							// Other Patient Names
- (0010,1010)							// Patient's Age
- (0010,1020)							// Patient's Size
- (0010,1030)							// Patient's Weight
- (0010,1090)							// Medical Record Locator
- (0010,2160)							// Ethnic Group
- (0010,2180)							// Occupation
- (0010,21B0)							// Additional Patient's History
- (0010,4000)							// Patient Comments
(0018,1000) := "(deidentified)"		// Device Serial Number
- (0018,1030)							// Protocol Name
(0020,000D) := new UID		// Study Instance UID
(0020,000E) := new UID		// Series Instance UID
- (0020,0010)							// Study ID
(0020,0052) := new UID		// Frame of Reference UID
(0020,0200) := new UID		// Synchronization Frame of Reference UID
- (0020,4000)							// Image Comments
- (0040,0275)							// Request Attributes Sequence
(0040,A124) := new UID		// Content Item UID
- (0040,A730)							// Content Sequence
- (0088,0140)							// Storage Media File-set UID
- (3006,0024)							// Referenced Frame of Reference UID
- (3006,00C2)							// Related Frame of Reference UID
(0012,0063) := "DicomBrowser metainfo basic deidentification"

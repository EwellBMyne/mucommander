package com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.SevenZip;

import java.io.IOException;

import com.mucommander.commons.file.impl.sevenzip.provider.Common.ObjectVector;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.HRESULT;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.ICompressProgressInfo;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.IInStream;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.IArchiveExtractCallback;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.IInArchive;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.SevenZipEntry;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Common.LocalCompressProgressInfo;
import com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Common.LocalProgress;


public class Handler implements com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.IInArchive {
    
    // useless LongVector _fileInfoPopIDs = new LongVector();
    
    IInStream _inStream;
    
    ArchiveDatabaseEx _database;
    
    int _numThreads;
    
    public Handler() {
        _numThreads = 1; // TBD
        
        _database = new ArchiveDatabaseEx();
        
    }
    
    public int Open(IInStream stream) throws IOException {
        return Open(stream,kMaxCheckStartPosition);
    }
    
    
    public int Open(
            IInStream stream, // InStream *stream
            long maxCheckStartPosition // const UInt64 *maxCheckStartPosition,
            // IArchiveOpenCallback *openArchiveCallback */
            ) throws IOException {
        close();
        
        // useless _fileInfoPopIDs.clear();
        // TBD try
        {
            // TBD CMyComPtr<IArchiveOpenCallback> openArchiveCallbackTemp = openArchiveCallback;
            
            /* TBD
            CMyComPtr<ICryptoGetTextPassword> getTextPassword;
            if (openArchiveCallback)
            {
              openArchiveCallbackTemp.QueryInterface(
                  IID_ICryptoGetTextPassword, &getTextPassword);
            }
             */
            
            InArchive archive = new InArchive();
            int ret = archive.Open(stream, maxCheckStartPosition);
            if (ret != HRESULT.S_OK) return ret;
            ret = archive.ReadDatabase(_database); // getTextPassword
            if (ret != HRESULT.S_OK) return ret;
            _database.Fill();
            _inStream = stream;
        }
        
        // FillPopIDs(); // useless _fileInfoPopIDs
        
        return HRESULT.S_OK;
    }
    
    public int Extract(int [] indices, int numItems,
            int testModeSpec, IArchiveExtractCallback extractCallbackSpec) throws IOException {
        
        boolean testMode = (testModeSpec != 0);
        long importantTotalUnPacked = 0;
        
        boolean allFilesMode = (numItems == -1);
        if (allFilesMode)
            numItems =
                    // #ifdef _7Z_VOL
                    // _refs.Size();
                    // #else
                    _database.Files.size();
        // #endif
        
        if(numItems == 0)
            return HRESULT.S_OK;
        
        ObjectVector<ExtractFolderInfo> extractFolderInfoVector = new ObjectVector<>();
        for(int ii = 0; ii < numItems; ii++) {
            int ref2Index = allFilesMode ? ii : indices[ii];
            
            {
        /*
      #ifdef _7Z_VOL
      // const CRef &ref = ref2.Refs[ri];
      const CRef &ref = _refs[ref2Index];
         
      int volumeIndex = ref.VolumeIndex;
      const CVolume &volume = _volumes[volumeIndex];
      const CArchiveDatabaseEx &database = volume.Database;
      UInt32 fileIndex = ref.ItemIndex;
      #else
         */
                ArchiveDatabaseEx database = _database;
                //#endif
                
                int folderIndex = database.fileIndexToFolderIndexMap.get(ref2Index);
                if (folderIndex == com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.SevenZip.InArchive.kNumNoIndex) {
                    extractFolderInfoVector.add( new ExtractFolderInfo(
                            // #ifdef _7Z_VOL
                            // volumeIndex,
                            // #endif
                            ref2Index, com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.SevenZip.InArchive.kNumNoIndex));
                    continue;
                }
                if (extractFolderInfoVector.isEmpty() ||
                        folderIndex != extractFolderInfoVector.Back().FolderIndex
        /*
        #ifdef _7Z_VOL
        || volumeIndex != extractFolderInfoVector.Back().VolumeIndex
        #endif
         */
                        ) {
                    extractFolderInfoVector.add( new ExtractFolderInfo(
            /*
            #ifdef _7Z_VOL
            volumeIndex,
            #endif
             */
                            com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.SevenZip.InArchive.kNumNoIndex, folderIndex));
                    Folder folderInfo = database.Folders.get(folderIndex);
                    long unPackSize = folderInfo.GetUnPackSize();
                    importantTotalUnPacked += unPackSize;
                    extractFolderInfoVector.Back().UnPackSize = unPackSize;
                }
                
                ExtractFolderInfo efi = extractFolderInfoVector.Back();
                
                // const CFolderInfo &folderInfo = m_dam_Folders[folderIndex];
                int startIndex = database.folderStartFileIndex.get(folderIndex); // CNum
                for (int index = efi.ExtractStatuses.size();
                index <= ref2Index - startIndex; index++) {
                    // UInt64 unPackSize = _database.Files[startIndex + index].UnPackSize;
                    // Count partial_folder_size
                    // efi.UnPackSize += unPackSize;
                    // importantTotalUnPacked += unPackSize;
                    efi.ExtractStatuses.add(index == ref2Index - startIndex);
                }
            }
        }
        
        extractCallbackSpec.SetTotal(importantTotalUnPacked);
        
        Decoder decoder = new Decoder(
                // #ifdef _ST_MODE
                false
                // #else
                // true
                // #endif
                );
        
        long currentImportantTotalUnPacked = 0;
        long totalFolderUnPacked;
        
        for(int i = 0; i < extractFolderInfoVector.size(); i++,
                currentImportantTotalUnPacked += totalFolderUnPacked) {
            ExtractFolderInfo efi = extractFolderInfoVector.get(i);
            totalFolderUnPacked = efi.UnPackSize;
            
            int res = extractCallbackSpec.SetCompleted(currentImportantTotalUnPacked);
            if (res != HRESULT.S_OK) return res;

            FolderOutStream folderOutStream = new FolderOutStream();

            // #ifdef _7Z_VOL
            // const CVolume &volume = _volumes[efi.VolumeIndex];
            // const CArchiveDatabaseEx &database = volume.Database;
            // #else
            ArchiveDatabaseEx database = _database;
            //#endif
            
            int startIndex; // CNum
            if (efi.FileIndex != InArchive.kNumNoIndex)
                startIndex = efi.FileIndex;
            else
                startIndex = database.folderStartFileIndex.get(efi.FolderIndex);
            
            
            int result = folderOutStream.Init(database,
                    // #ifdef _7Z_VOL
                    // volume.StartRef2Index,
                    // #else
                    0,
                    // #endif
                    startIndex,
                    efi.ExtractStatuses, extractCallbackSpec, testMode);
            
            if (result != HRESULT.S_OK) return result;
            
            if (efi.FileIndex != InArchive.kNumNoIndex)
                continue;
            
            int folderIndex = efi.FolderIndex; // CNum
            Folder folderInfo = database.Folders.get(folderIndex);
            
            LocalProgress localProgressSpec = new LocalProgress();
            localProgressSpec.Init(extractCallbackSpec, false);
            
            LocalCompressProgressInfo localCompressProgressSpec =
                    new LocalCompressProgressInfo();
            localCompressProgressSpec.Init(localProgressSpec, ICompressProgressInfo.INVALID , currentImportantTotalUnPacked);
            
            int packStreamIndex = database.folderStartPackStreamIndex.get(folderIndex); // CNum
            long folderStartPackPos = database.GetFolderStreamPos(folderIndex, 0);
            
    /*
    #ifndef _NO_CRYPTO
    CMyComPtr<ICryptoGetTextPassword> getTextPassword;
    if (extractCallback)
      extractCallback.QueryInterface(IID_ICryptoGetTextPassword, &getTextPassword);
    #endif
     */
            
            try {
                result = decoder.Decode(
                        // #ifdef _7Z_VOL
                        // volume.Stream,
                        // #else
                        _inStream,
                        // #endif
                        folderStartPackPos,
                        database.PackSizes,  // database.PackSizes.get(packStreamIndex),
                        packStreamIndex,
                        folderInfo,
                        folderOutStream,
                        localCompressProgressSpec
                        // #ifndef _NO_CRYPTO
                        // , getTextPassword
                        // #endif
                        // #ifdef COMPRESS_MT
                        // , true, _numThreads
                        // #endif
                        );
                
                if (result == HRESULT.S_FALSE) {
                    result = folderOutStream.FlushCorrupted(IInArchive.NExtract_NOperationResult_kDataError);
                    if (result != HRESULT.S_OK) return result;
                    continue;
                }
                if (result == HRESULT.E_NOTIMPL) {
                    result = folderOutStream.FlushCorrupted(IInArchive.NExtract_NOperationResult_kUnSupportedMethod);
                    if (result != HRESULT.S_OK) return result;
                    continue;
                }
                if (result != HRESULT.S_OK)
                    return result;
                if (folderOutStream.WasWritingFinished() != HRESULT.S_OK) {
                    result = folderOutStream.FlushCorrupted(IInArchive.NExtract_NOperationResult_kDataError);
                    if (result != HRESULT.S_OK) return result;
                }
            } catch(Exception e) {
                System.out.println("IOException : " + e);
                e.printStackTrace();
                result = folderOutStream.FlushCorrupted(IInArchive.NExtract_NOperationResult_kDataError);
                if (result != HRESULT.S_OK) return result;
            }
        }
        return HRESULT.S_OK;
    }
    
    
    public int close() throws IOException {
/*
        #ifdef _7Z_VOL
        _volumes.Clear();
        _refs.Clear();
        #else
        _inStream.Release();
        _database.Clear();
        #endif
        return S_OK;
 */
        if (_inStream != null) _inStream.close();  // _inStream.Release();
        _inStream = null;
        _database.Clear();
        return 0;
    }
    
    public int size() {
        return _database.Files.size();
    }
    
    long getPackSize(int index2) {
        long packSize = 0;
        int folderIndex = _database.fileIndexToFolderIndexMap.get(index2);
        if (folderIndex != InArchive.kNumNoIndex) {
            if (_database.folderStartFileIndex.get(folderIndex) == index2)
                packSize = _database.GetFolderFullPackSize(folderIndex);
        }
        return packSize;
    }
    
    static int GetUInt32FromMemLE(byte [] p , int off) {
        return p[off] | (((int)p[off + 1]) << 8) | (((int)p[off + 2]) << 16) | (((int)p[off +3]) << 24);
    }
    
    static String GetStringForSizeValue(int value) {
        for (int i = 31; i >= 0; i--) {
            if ((1 << i) == value) {
                return "" + i;
            }
        }
        String result = "";
        if (value % (1 << 20) == 0) {
            result += "" + (value >> 20);
            result += "m";
        } else if (value % (1 << 10) == 0) {
            result += "" + (value >> 10);
            result += "k";
        } else {
            result += "" + (value);
            result += "b";
        }
        return result;
    }
    
    String getMethods(int index2) {
        int folderIndex = _database.fileIndexToFolderIndexMap.get(index2);
        if (folderIndex != InArchive.kNumNoIndex) {
            Folder folderInfo = _database.Folders.get(folderIndex);
            StringBuilder methodsString = new StringBuilder();
            for (int i = folderInfo.Coders.size() - 1; i >= 0; i--) {
                CoderInfo coderInfo = folderInfo.Coders.get(i);
                if (methodsString.length() != 0) {
                    methodsString.append(' ');
                }
                
                // MethodInfo methodInfo;

                for (int j = 0; j < coderInfo.AltCoders.size(); j++) {
                    if (j > 0) {
                        methodsString.append("|");
                    }
                    AltCoderInfo altCoderInfo = coderInfo.AltCoders.get(j);
                    
                    String methodName = getMethodName(altCoderInfo);
                    if (methodName != null) {
                        methodsString.append(methodName);
                        
                        if (altCoderInfo.MethodID.equals(MethodID.k_LZMA)) {
                            if (altCoderInfo.Properties.GetCapacity() >= 5) {
                                methodsString.append(":");
                                int dicSize = GetUInt32FromMemLE(altCoderInfo.Properties.data(),1);
                                methodsString.append(GetStringForSizeValue(dicSize));
                            }
                        }
                        /* else if (altCoderInfo.MethodID == k_PPMD) {
                            if (altCoderInfo.Properties.GetCapacity() >= 5) {
                                Byte order = *(const Byte *)altCoderInfo.Properties;
                                methodsString += ":o";
                                methodsString += ConvertUInt32ToString(order);
                                methodsString += ":mem";
                                UInt32 dicSize = GetUInt32FromMemLE(
                                        ((const Byte *)altCoderInfo.Properties + 1));
                                methodsString += GetStringForSizeValue(dicSize);
                            }
                        } else if (altCoderInfo.MethodID == k_AES) {
                            if (altCoderInfo.Properties.GetCapacity() >= 1) {
                                methodsString += ":";
                                const Byte *data = (const Byte *)altCoderInfo.Properties;
                                Byte firstByte = *data++;
                                UInt32 numCyclesPower = firstByte & 0x3F;
                                methodsString += ConvertUInt32ToString(numCyclesPower);
                            }
                        } else {
                            if (altCoderInfo.Properties.GetCapacity() > 0) {
                                methodsString += ":[";
                                for (size_t bi = 0; bi < altCoderInfo.Properties.GetCapacity(); bi++) {
                                    if (bi > 5 && bi + 1 < altCoderInfo.Properties.GetCapacity()) {
                                        methodsString += "..";
                                        break;
                                    } else
                                        methodsString += GetHex2(altCoderInfo.Properties[bi]);
                                }
                                methodsString += "]";
                            }
                        }
                         */
                    } else {
                        // TBD methodsString += altCoderInfo.MethodID.ConvertToString();
                    }
                }
            }
            return methodsString.toString();
        }
        
        return "";
    }

    private String getMethodName(AltCoderInfo altCoderInfo) {
        if (altCoderInfo.MethodID.equals(MethodID.k_Copy)) {
            return "Copy";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_LZMA)) {
            return "LZMA";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_BCJ)) {
            return "BCJ";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_BCJ2)) {
            return "BCJ2";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_PPMD)) {
            return "PPMD";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_Deflate)) {
            return "Deflate";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_Deflate64)) {
            return "Deflate64";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_BZip2)) {
            return "BZip2";
        } else if (altCoderInfo.MethodID.equals(MethodID.k_7zAES)) {
            return "7zAES";
        } else {
            return null;
        }
    }

    public SevenZipEntry getEntry(int index) {
        com.mucommander.commons.file.impl.sevenzip.provider.SevenZip.Archive.SevenZip.FileItem item = _database.Files.get(index);

        long crc = -1;
        if (item.IsFileCRCDefined) {
            crc = item.FileCRC & 0xFFFFFFFFL;
        }
        
        long position = -1;
        if (item.IsStartPosDefined)
            position = item.StartPos;

        return new SevenZipEntry(
                item.name,
                getPackSize(index),
                item.UnPackSize,
                crc,
                item.LastWriteTime,
                position,
                item.IsDirectory,
                item.Attributes,
                getMethods(index)
                );
    }
    
}
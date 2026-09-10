# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# iText / POI use reflection heavily
-dontwarn com.itextpdf.**
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**

package com.novelwriter.app.data.model

// Item type in the project tree
enum class NWItemType { ROOT, FOLDER, FILE }

// Content category
enum class NWItemClass {
    NOVEL, PLOT, CHARACTER, WORLD, TIMELINE, OBJECT, ENTITY, CUSTOM, ARCHIVE, TEMPLATE, TRASH
}

// Document layout
enum class NWItemLayout { DOCUMENT, NOTE }

// An entry in the nwProject.nwx item tree
data class NWItem(
    val handle: String,
    val parent: String?,         // null if top-level (parent="None")
    val root: String,
    val order: Int,
    val type: NWItemType,
    val itemClass: NWItemClass,
    val layout: NWItemLayout?,
    val name: String,
    val wordCount: Int,
    val charCount: Int,
    val active: Boolean
)

// Parsed novelWriter project metadata + tree
data class NWProject(
    val id: String,
    val name: String,
    val author: String,
    val items: List<NWItem>
)

// A .nwd document's parsed content
data class NWDocument(
    val handle: String,
    val name: String,
    val kind: String,
    val content: String        // everything after the %%~ header lines
)

// How the project is synced to the cloud
enum class StorageType { GIT, CLOUD_FOLDER }

// A locally stored novel project
data class LocalProject(
    val name: String,
    val path: String,
    val remoteUrl: String? = null,
    val storageType: StorageType = StorageType.GIT,
    val cloudUri: String? = null   // SAF tree URI for CLOUD_FOLDER projects
)

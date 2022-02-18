# Tubes1_Stima
Tugas Besar I IF2211 Strategi Algoritma Semester II Tahun 2021/2022 Pemanfaatan Algoritma Greedy dalam Aplikasi Permainan "Overdrive"

## Daftar Isi
* [Deskripsi Singkat Program](#deskripsi-singkat-tugas)
* [Strategi Greedy Program](#strategi-greedy-program)
* [Struktur Program](#struktur-program)
* [Requirement Program](#requirement-program)
* [Cara Kompilasi Program](#cara-kompilasi-program)
* [Cara Menjalankan Program](#cara-menjalankan-program)
* [Link Demo Program](#link-demo-program)
* [Author Program](#author-program)

## Deskripsi Singkat Tugas
Overdrive adalah sebuah game yang mempertandingan 2 *bot* mobil dalam sebuah ajang balapan. 
Setiap pemain akan memiliki sebuah *bot* mobil dan masing-masing *bot* akan saling bertanding untuk mencapai garis *finish* dan memenangkan pertandingan. 
Agar dapat memenangkan pertandingan, setiap pemain harus mengimplementasikan strategi tertentu untuk dapat mengalahkan lawannya.
Bahasa pemrograman yang digunakan pada tugas besar ini adalah Java. 
Bahasa Java tersebut digunakan untuk membuat algoritma pada *bot*. 
IDE yang digunakan untuk membantu membuat projek ini adalah Intellij IDEA. 
Intellij IDEA merupakan IDE yang kompatibel dengan bahasa Java, dikarenakan beberapa *tools*-nya seperti Maven sudah *built in* tanpa perlu menambahkan *extension*. 
Untuk menjalankan permainan, digunakan sebuah *game engine* yang diciptakan oleh Entellect Challenge yang terdapat pada *repository* githubnya. 
Game engine yang dibuat oleh Entellect Challenge menggunakan bahasa Scala sebagai bahasa pemrograman utama dalam pembuatannya.

## Strategi Greedy Program
Dalam permainan Overdrive, tujuan setiap *bot* pemain berusaha untuk menjadi yang pertama melewati *finish line*. 
Terdapat banyak cara untuk meraih hal tersebut, seperti berusaha mencapai kecepatan maksimum pada setiap waktu, membuat *bot* lawan tidak bisa bergerak/bergerak lebih lamban, dan lain-lain.

### Fix and Damage Mechanism
Salah satu subpermasalahan dalam permainan Overdrive adalah adanya *damage mechanism*.
*Car* pemain dapat menerima *damage* jika *car* tersebut melakukan *collisions* dengan beberapa *block* khusus (*Oil Spill*, *Cyber Truck*, *Flimsy Wall*, dan *Mud*) atau *car* lawannya. 
Jumlah *damage* yang diterima pemain tersebut mempengaruhi *speed* maksimum dari *car*. 
Untuk menghilangkan *damage*, pemain dapat melakukan *command FIX* untuk menghilangkan 2 poin *damage* dengan bayaran *car* tidak bergerak selama ronde berikutnya.

### Speed and Boost Power Up
Salah satu subpermasalahan yang paling penting dan mendasar pada permainan Overdrive adalah subpermasalahan *speed mechanism*. 
Tujuan utama dari permainan Overdrive adalah mendahului *bot* lawan untuk mencapai *finish line* terlebih dahulu. 
Oleh karena itu, dibutuhkan algoritma *greedy* yang menangani *speed mechanism* secara efektif dan efisien.

### Obstacle Avoidance and Lizard Power Up
Kemudian, subpermasalahan selanjutnya yang dapat diselesaikan oleh algoritma *greedy* adalah subpermasalahan penghindaran suatu *obstacle* dengan cara perpindahan *lane* atau dengan penggunaan *lizard power up*. 
Hal ini berguna pula untuk menghindari *damage* serta pengurangan *speed* akibat *collisions* antara *car* pemain dengan *block* khusus (*Oil spill*, *Cyber truck*, *Flimsy Wall*, dan *Mud*) ataupun car lawan. 
*Lizard power up* sendiri memiliki efek *car* akan melompati semua *power up pick-ups*, *obstacles*, serta *car* lawan selama ronde tersebut berlangsung dengan *car* pemain masih terdapat pada *lane* yang sama.

### Power Up Pick-Ups
Selanjutnya, subpermasalahan yang dapat diselesaikan dengan algoritma *greedy* adalah subpermasalahan bagaimana cara mendapatkan *power up pick-ups* paling banyak dengan cara perpindahan *lane*. 
Meskipun sekilas subpermasalahan ini tidak seurgensi subpermasalahan lain seperti penghindaran *obstacle* ataupun *speed mechanism*, tetapi *bot* pemain juga membutuhkan bantuan *power up* untuk memenangkan permainan. 
Selain itu, bila *bot* pemain tidak mengambil *power up pick-ups*, maka *bot* lawan akan mendapatkan peluang untuk mengambil *power up pick-ups* tersebut dan menggunakannya untuk mengalahkan *bot* pemain.

### Offensive Power Up
Terakhir, subpermasalahan yang dapat diselesaikan dengan algoritam *greedy* adalah subpermasalahan penggunaan *power up* yang bersifat "ofensif" secara efektif dan efisien. 
Harapannya, setiap *command USE_<POWER_UP>* yang dieksekusi dapat mengenai *car* lawan dan mengakibatkan efek paling kentara. 
Hal ini dilakukan agar *power up pick-ups* yang telah diambil sebelumnya tidak terbuang sia-sia serta tidak terjadi penumpukan *power up* pada *inventory*.

## Struktur Program

## Requirement Program

## Cara Kompilasi Program

## Cara Menjalankan Program

## Link Demo Program
* https://www.youtube.com/watch?v=5rKVX4CT4i0

## Author Program
* [Rayhan Kinan Muhannad - 13520065](https://github.com/rayhankinan)
* [Andhika Arta Aryanto](https://github.com/dhikaarta)
* [Aira Thalca Avila Putra](https://github.com/airathalca)
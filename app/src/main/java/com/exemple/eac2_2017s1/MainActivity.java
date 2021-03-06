package com.exemple.eac2_2017s1;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.exemple.eac2_2017s1.XmlParser.Entrada;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // RSS FEED
    //private static final String URL = "http://estaticos.marca.com/rss/portada.xml";
    //private static final String URL = "http://balita.net.ph/feed/";
    private static final String URL = "http://inquirer.net/fullfeed/";

    //Inicializacion
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Adaptador adapter;
    private DBInterface db;
    private List<Entrada> listSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Referenciem el RecyclerView
        recyclerView = (RecyclerView) findViewById(R.id.rView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //afegim l'adaptador amb les dades i el LinearLayoutManager que pintarà les dades
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adaptador(this);
        recyclerView.setAdapter(adapter);

        //Podem utilitzar animacions sobre la llista
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        //DB
        db = new DBInterface(this);
        listSearch = new ArrayList<>();

        //Referenciamos y añadimos listener para buscar
        ImageButton imageButtonBusqueda = (ImageButton) findViewById(R.id.imageButtonBusqueda);
        imageButtonBusqueda.setOnClickListener(this);

        //Carreguem les noticies a un fil independent fent servir AsyncTask
        cargaNoticias();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //Si se presiona el boton actualizar actualiza el estado de la red y carga las noticias
        if (id == R.id.action_actualizar) {
            cargaNoticias();
            return true;
            //If the search button is pressed, filter the list
        } else if (id == R.id.action_buscar) {
            mostrarBarraBusqueda();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void mostrarBarraBusqueda() {
        LinearLayout barraBusqueda = (LinearLayout) findViewById(R.id.barraBusqueda);
        if (barraBusqueda.getVisibility() == View.GONE) {
            barraBusqueda.setVisibility(View.VISIBLE);
        } else {
            barraBusqueda.setVisibility(View.GONE);
        }
    }

    //Fa servir AsyncTask per descarregar el feed XML de stackoverflow.com
    public void cargaNoticias() {
        //Si tenim connexió al dispositiu
        if (Util.hayConexion(this)) {
            new DownloadTask().execute(URL);
        } else {
            Toast.makeText(this, "No hi ha connexio", Toast.LENGTH_LONG).show();
            List<Entrada> lista = cargarDB();
            adapter.setList(lista);
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            //atualizamos el listado local para busquedas
            listSearch = lista;
        }
    }

    private void downloadImages(List<Entrada> result) {
        for (Entrada entrada : result) {
            try {
                // Let's take the URL that has been passed as an argument
                URL imatge = new URL(entrada.imagen);
                // We create the input and a buffer where we will read the information
                InputStream inputstream = (InputStream) imatge.getContent();
                byte[] bufferImatge = new byte[1024];
                // Creem la sortida, és a dir, allà on guardarem la informació (ruta de la imatge)
                String path = getCacheDir().toString() + File.separator
                        + entrada.imagen.substring(entrada.imagen.lastIndexOf('/') + 1, entrada.imagen.length());
                OutputStream outputstream = new FileOutputStream(path);
                int count;
                // Mentre hi hagi informació que llegir
                while ((count = inputstream.read(bufferImatge)) != -1) {
                    // Guardem al disc el que hem descarregat
                    outputstream.write(bufferImatge, 0, count);
                }
                // Tanquem els "stream"
                inputstream.close();
                outputstream.close();
            } catch (IOException exception) {
                Log.d("ERR", "Alguna cosa no ha anat bé!");
            }
        }
    }

    //Descarrega XML, l'analitza i crea amb ell un codi HTML que retorna com String
    //Download XML, l'analitza and create an HTML code that returns with String
    private List<Entrada> carregaXMLdelaXarxa(String urlString) throws XmlPullParserException, IOException {

        List<Entrada> entradas = null;
        InputStream stream = null;
        //Creem una instncia de l'analitzador
        XmlParser analitzador = new XmlParser();
        //Cadena on construirem el codi HTML que mostrara el widget webView
        StringBuilder htmlString = new StringBuilder();

        try {

            //Obrim la connexio
            //stream = obreConnexioHTTP(urlString);
            stream = ObreConnexioHTTP(urlString);

            //Obtenim la llista d'entrades a partir de l'stream de dades
            //Get the list of entries from the stream of data
            entradas = analitzador.analitza(stream);
            Log.d("marker2", "doInBackground: error here?");
            //adapter.update(entrades);
            //adapter.notifyDataSetChanged();

        } catch (Exception e) {
            //Toast.makeText(getBaseContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            //Tanquem l'stream una vegada hem terminat de treballar amb ell
            if (stream != null) {
                stream.close();

            }
        }

        return entradas;

    }

    //Obre una connexió HTTP a partir d'un URL i retorna un InputStream
    private InputStream ObreConnexioHTTP(String adrecaURL) throws IOException {
        InputStream in = null;        //Buffer de recepció
        int resposta = -1;            //Resposta de la connexió

        //Obtenim un URL a partir de l'String proporcionat
        URL url = new URL(adrecaURL);

        //Obtenim una nova connexió al recurs referenciat per la URL
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        try {
            ///////////////////////
            //Preparem la petició//
            ///////////////////////

            httpConn.setReadTimeout(10000);            //Timeout de lectura en milisegons
            httpConn.setConnectTimeout(15000);        //Timeout de connexió en milisegons
            httpConn.setRequestMethod("GET");        //Petició al servidor
            httpConn.setDoInput(true);                //Si la connexió permet entrada

            //Es connecta al recurs.
            httpConn.connect();

            //Obtenim el codi de resposta obtingut del servidor remot HTTP
            resposta = httpConn.getResponseCode();

            //Comprovem si el servidor ens ha retornat un codi de resposta OK,
            //que correspon a que el contingut s'ha descarregat correctament
            if (resposta == HttpURLConnection.HTTP_OK) {
                //Obtenim un Input stream per llegir del servidor
                //in = new BufferedInputStream(httpConn.getInputStream());
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            //Hi ha hagut un problema al connectar
            throw new IOException("Error connectant");
        }

        //Retornem el flux de dades
        return in;
    }

    public void dbInsertAll(List<Entrada> result) {
        db.dropAndRecreateTable();
        db.open();
        for (Entrada r : result) {
            String titulo = r.titulo;
            String enlace = r.enlace;
            String autor = r.autor;
            String descripcion = r.descripcion;
            String fecha = r.fecha;
            String categoria = r.categoria;
            String imagen = r.imagen;
            db.insert(titulo, descripcion, enlace, autor, fecha, categoria, imagen);
        }
        db.close();
    }

    protected List<Entrada> cargarDB() {
        List<Entrada> entradas = new ArrayList<>();

        db.open();
        Cursor cursor = db.getAll();
        
        while (cursor.moveToNext()) {
            String titulo = cursor.getString(cursor.getColumnIndex("titulo"));
            String enlace = cursor.getString(cursor.getColumnIndex("enlace"));
            String autor = cursor.getString(cursor.getColumnIndex("autor"));
            String descripcion = cursor.getString(cursor.getColumnIndex("descripcion"));
            String fecha = cursor.getString(cursor.getColumnIndex("fecha"));
            String categoria = cursor.getString(cursor.getColumnIndex("categoria"));
            String imagen = cursor.getString(cursor.getColumnIndex("imagen"));
            entradas.add(new Entrada(titulo, descripcion, enlace, autor, fecha, categoria, imagen));
        }
        return entradas;
    }

    @Override
    public void onClick(View v) {
        //usa el listado local generando una nueva lista y pasandola al adaptador
        List<Entrada> listaFiltrada = new ArrayList<>();
        TextView editTextBusqueda = (TextView) findViewById(R.id.editTextBusqueda);
        String busqueda = editTextBusqueda.getText().toString().toLowerCase();
        for (Entrada e : listSearch) {
            if (e.titulo.toLowerCase().contains(busqueda))
                listaFiltrada.add(e);
        }
        adapter.setList(listaFiltrada);
        adapter.notifyDataSetChanged();
    }

    //Implementació d'AsyncTask per descarregar el feed XML
    private class DownloadTask extends AsyncTask<String, Void, List<Entrada>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        }

        @Override
        //What will run in the background
        protected List<Entrada> doInBackground(String... urls) {

            List<Entrada> lista = null;

            try {
                //Let's load the XML
                Log.d("marker1", "doInBackground: error here?");
                lista = carregaXMLdelaXarxa(urls[0]);

                //we insert in the db
                dbInsertAll(lista);

                //download the images
                downloadImages(lista);

            } catch (IOException | XmlPullParserException e) {
                //Error

            }

            return lista;

        }


        @Override
        //Una vegada descarregada la informació XML i convertida a HTML l'enllacem al WebView
        protected void onPostExecute(List<Entrada> lista) {
            adapter.setList(lista);
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            //atualizamos el listado local para busquedas
            listSearch = lista;
        }

    }

}

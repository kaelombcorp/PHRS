package com.exemple.eac2_2017s1;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BlueStorm on 12/10/2017.
 */

public class XmlParser {

    // We do not use namespaces
    private static final String ns = null;

    public List<Entrada> analitza(InputStream in) throws XmlPullParserException, IOException {
        try {
            //We get analyzer
            XmlPullParser parser = Xml.newPullParser();
            //We do not use namespaces
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            //Specifies the parser input
            parser.setInput(in, null);
            //We get the first label
            parser.nextTag();
            //Retornem la llista de noticies
            return leerNoticias(parser);
        } finally {
            in.close();
        }
    }

    //Reads a list of StackOverflow news from the parser and returns a list of Entries
    private List<Entrada> leerNoticias(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Entrada> listaItems = new ArrayList<Entrada>();
        //Check if the current event is of the expected type (START_TAG) and the name "feed"
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, ns, "channel");
        //Mentre que no arribem al final d'etiqueta

        while (parser.next() != XmlPullParser.END_TAG) {
            //We ignore all events that are not a tag start
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                //Saltem al seguent event
                continue;
            }
            //We get the name of the label
            String name = parser.getName();
            // If this tag is a news post
            if (name.equals("item")) {
                //We add the entry to the list
                listaItems.add(leerItem(parser));
            } else {
                saltar(parser);
            }
        }
        return listaItems;
    }

    //This function is used to skip a tag and its nested sub-tags.
    private void saltar(XmlPullParser parser) throws XmlPullParserException, IOException {
        //Si no és un comenament d'etiqueta: ERROR
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;

        //Check that you have gone through as many start tags as finishing tags

        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    //Cada vegada que es tanca una etiqueta resta 1
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    //Cada vegada que s'obre una etiqueta augmenta 1
                    depth++;
                    break;
            }
        }
    }

    //Analyzes the content of an entry. If you find a title, summary or link, call the reading methods
    //to process them. If not, ignore the tag.
    private Entrada leerItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        String titulo = null;
        String enlace = null;
        String autor = null;
        String descripcion = null;
        String fecha = null;
        String categoria = null;
        String imagen = null;

        //Current label must be "item"
        parser.require(XmlPullParser.START_TAG, ns, "item");

        //While the "item" tag doesn't end
        while (parser.next() != XmlPullParser.END_TAG) {
            //Ignore until we find a label start
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            //We get the name of the label
            String etiqueta = parser.getName();
            if (etiqueta.equals("title")) { //title news
                titulo = leerEtiqueta(parser, "title");
            } else if (etiqueta.equals("media:description")) { // summary
                descripcion = leerEtiqueta(parser, "media:description");
            } else if (etiqueta.equals("link")) { //enlace
                enlace = leerEtiqueta(parser, "link");
            } else if (etiqueta.equals("pubDate")) { //date
                fecha = leerEtiqueta(parser, "pubDate");
            } else if (etiqueta.equals("dc:creator")) { //Author
                autor = leerEtiqueta(parser, "dc:creator");
            } else if (etiqueta.equals("category")) { //category
                if (categoria == null) {
                    categoria = (leerEtiqueta(parser, "category"));
                } else {
                    categoria += (", " + leerEtiqueta(parser, "category"));
                }
            } else if (etiqueta.equals("media:thumbnail")) { //image
                imagen = leerImagen(parser);
            } else {
                //the other labels we skip
                saltar(parser);
            }
        }
        //We create a new entry with this data and return it
        return new Entrada(titulo, descripcion, enlace, autor, fecha, categoria, imagen);
    }

    private String leerEtiqueta(XmlPullParser parser, String etiqueta) throws IOException, XmlPullParserException {
        //Current label must be "pubDate"
        parser.require(XmlPullParser.START_TAG, ns, etiqueta);
        //reads
        String contenido = llegeixText(parser);
        //End of label
        parser.require(XmlPullParser.END_TAG, ns, etiqueta);
        return contenido;
    }

    private String leerImagen(XmlPullParser parser) throws IOException, XmlPullParserException {
        //The current label should be "media: thumbnail"
        parser.require(XmlPullParser.START_TAG, ns, "media:thumbnail");
        //Read URL attribute
        String imagen = parser.getAttributeValue(null, "url");
        //Fi label has no
        parser.next();
        return imagen;
    }

    // Extract the text value from the title, summary tags
    private String llegeixText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String resultat = "";

        if (parser.next() == XmlPullParser.TEXT) {
            resultat = parser.getText();
            parser.nextTag();
        }
        return resultat;
    }

    //This class represents a RSS Feed news entry
    public static class Entrada implements Serializable {
        public final String titulo;
        public final String enlace;
        public final String autor;
        public final String descripcion;
        public final String fecha;
        public final String categoria;
        public final String imagen;

        public Entrada(String titulo, String descripcion, String enlace, String autor, String fecha, String categoria, String imagen) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.enlace = enlace;
            this.autor = autor;
            this.fecha = fecha;
            this.categoria = categoria;
            this.imagen = imagen;
        }
    }

}
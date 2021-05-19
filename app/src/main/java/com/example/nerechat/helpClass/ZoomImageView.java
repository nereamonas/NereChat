package com.example.nerechat.helpClass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
//Copiado de https://github.com/Destroyer716/ZoomImageTest

//ImageView donde se puede hacer zoom

public class ZoomImageView extends ImageView implements View.OnTouchListener {

    public class ZoomMode{
        public  final  static  int Ordinary=0;
        public  final  static  int  ZoomIn=1;
        public final static int TowFingerZoom = 2;
    }



    private Matrix matrix;
    // tamaño imageView
    private PointF viewSize;
    // El tamaño de la imagen
    private PointF imageSize;
    // El tamaño de la imagen después de escalar
    private PointF scaleSize = new PointF();
    // La relación de zoom inicial de ancho y alto
    private PointF originScale = new PointF();
    // xy coordenadas en tiempo real del mapa de bits en la vista de imagen
    private PointF bitmapOriginPoint = new PointF();
    // Punto en el que se hizo clic
    private PointF clickPoint = new PointF();
    // Establecer el límite de tiempo de verificación de doble clic
    private long doubleClickTimeSpan = 250;
    // Hora del último clic
    private long lastClickTime = 0;
    // Haga doble clic para acercar
    private int doubleClickZoom = 2;
    // Modo de zoom actual
    private int zoomInMode = ZoomMode.Ordinary;
    // Datos de escala de coordenadas temporales
    private PointF tempPoint = new PointF();
    // Relación de zoom máxima
    private float maxScrole = 4;
    // La distancia entre dos puntos
    private float doublePointDistance = 0;
    // El punto central cuando dos dedos hacen zoom
    private PointF doublePointCenter = new PointF();
    // Relación de zoom con dos dedos
    private float doubleFingerScrole = 0;
    // Número de dedos tocados la última vez
    private int lastFingerNum = 0;


    public ZoomImageView(Context context) {
        super(context);
        init();
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        setOnTouchListener(this);
        setScaleType(ScaleType.MATRIX);
        matrix = new Matrix();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        viewSize = new PointF(width,height);

        Drawable drawable = getDrawable();
        if (drawable != null){
            imageSize = new PointF(drawable.getMinimumWidth(),drawable.getMinimumHeight());
            showCenter();
        }
    }

    /**
     * Configure la imagen para que se muestre en la proporción media
     */
    private void showCenter(){
        float scalex = viewSize.x/imageSize.x;
        float scaley = viewSize.y/imageSize.y;

        float scale = scalex<scaley?scalex:scaley;
        scaleImage(new PointF(scale,scale));

        // Mueve la imagen y guarda las coordenadas de la esquina superior izquierda (u origen) de la imagen original
        if (scalex<scaley){
            translationImage(new PointF(0,viewSize.y/2 - scaleSize.y/2));
            bitmapOriginPoint.x = 0;
            bitmapOriginPoint.y = viewSize.y/2 - scaleSize.y/2;
        }else {
            translationImage(new PointF(viewSize.x/2 - scaleSize.x/2,0));
            bitmapOriginPoint.x = viewSize.x/2 - scaleSize.x/2;
            bitmapOriginPoint.y = 0;
        }
        // Guarde la relación de zoom inicial
        originScale.set(scale,scale);
        doubleFingerScrole = scale;
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Evento de prensa de dedos
                // Registra las coordenadas del punto seleccionado
                clickPoint.set(event.getX(),event.getY());
                // Determine la cantidad de puntos que se mantienen presionados en la pantalla en este momento, se activa cuando solo se hace clic en un punto en la pantalla actual
                if (event.getPointerCount() == 1) {
                    // Establecer un intervalo de clic para determinar si es un doble clic
                    if (System.currentTimeMillis() - lastClickTime <= doubleClickTimeSpan) {
                        // Si el modo de zoom de la imagen es el modo normal en este momento, activará el doble clic para acercar
                        if (zoomInMode == ZoomMode.Ordinary) {
                            // Registre la relación de la distancia entre el punto en el que se hizo clic y la esquina superior izquierda de la imagen en el eje x, y y la longitud lateral de la imagen en el eje x, y, respectivamente,
                            // Es conveniente calcular el punto de coordenadas correspondiente a este punto después de hacer zoom
                            tempPoint.set((clickPoint.x - bitmapOriginPoint.x) / scaleSize.x,
                                    (clickPoint.y - bitmapOriginPoint.y) / scaleSize.y);
                            // Para hacer zoom
                            scaleImage(new PointF(originScale.x * doubleClickZoom,
                                    originScale.y * doubleClickZoom));
                            // Obtenga la coordenada xy de la esquina superior izquierda de la imagen después de hacer zoom
                            getBitmapOffset();

                            // Traduce la imagen para que la posición del punto en el que se hizo clic permanezca sin cambios. Aquí está la coordenada xy en la que se hizo clic después de hacer zoom,
                            // Calcula la diferencia entre el valor de la coordenada xy de la posición original en la que se hizo clic y luego realiza una operación de traducción
                            translationImage(
                                    new PointF(
                                            clickPoint.x - (bitmapOriginPoint.x + tempPoint.x * scaleSize.x),
                                            clickPoint.y - (bitmapOriginPoint.y + tempPoint.y * scaleSize.y))
                            );
                            zoomInMode = ZoomMode.ZoomIn;
                            doubleFingerScrole = originScale.x*doubleClickZoom;
                        } else {
                            // Haga doble clic para restaurar
                            showCenter();
                            zoomInMode = ZoomMode.Ordinary;
                        }
                    } else {
                        lastClickTime = System.currentTimeMillis();
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // Ya hay un punto en la pantalla para mantener presionado y presionar un punto para activar el evento
                // Calcula la distancia entre los dos primeros dedos
                doublePointDistance = getDoubleFingerDistance(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // Ya hay dos puntos en la pantalla que activarán el evento cuando presionas y sueltas un punto
                // Cuando un dedo sale de la pantalla, modifique el estado para que si toca dos veces la pantalla, se pueda restaurar a su tamaño original
                zoomInMode = ZoomMode.ZoomIn;
                // Registre la relación de zoom de dos dedos en este momento
                doubleFingerScrole =scaleSize.x/imageSize.x;
                // Registre el número de puntos tocados en la pantalla en este momento
                lastFingerNum = 1;
                // A juzgar por la proporción escalada, si es menor que la proporción original, se restaurará al tamaño original
                if (scaleSize.x<viewSize.x && scaleSize.y<viewSize.y){
                    zoomInMode = ZoomMode.Ordinary;
                    showCenter();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // El evento se activa cuando el dedo se mueve
                /**************************************Móvil
                 *******************************************/
                if (zoomInMode != ZoomMode.Ordinary) {
                    // Si es de varios dedos, el punto central calculado es el punto de clic supuesto
                    float currentX = 0;
                    float currentY = 0;
                    // Obtenga cuántos puntos se tocan en la pantalla en este momento
                    int pointCount = event.getPointerCount();
                    // Calcula las coordenadas del punto intermedio
                    for (int i = 0; i < pointCount; i++) {
                        currentX += event.getX(i);
                        currentY += event.getY(i);
                    }
                    currentX /= pointCount;
                    currentY /= pointCount;
                    // Cuando cambia el número de puntos tocados en la pantalla, considere el último punto central calculado como el punto donde se hizo clic
                    if (lastFingerNum != event.getPointerCount()) {
                        clickPoint.x = currentX;
                        clickPoint.y = currentY;
                        lastFingerNum = event.getPointerCount();
                    }
                    // Al mover el dedo, las coordenadas en tiempo real del punto central se calculan en tiempo real, menos las coordenadas del punto en el que se hizo clic para obtener la distancia que se debe mover
                    float moveX = currentX - clickPoint.x;
                    float moveY = currentY - clickPoint.y;
                    // Calcula el límite para que no pueda estar fuera del límite, pero si se mueve al hacer zoom con dos dedos, debido al efecto de zoom,
                    // Entonces, el juicio de límites en este momento no es válido
                    float[] moveFloat = moveBorderDistance(moveX, moveY);
                    // Manejar el evento de imágenes en movimiento
                    translationImage(new PointF(moveFloat[0], moveFloat[1]));
                    clickPoint.set(currentX, currentY);
                }
                /**************************************Enfocar
                 *******************************************/
                // Juzgar que dos dedos están tocando la pantalla antes de procesar el evento de zoom
                if (event.getPointerCount() == 2){
                    // Si el tamaño escalado en este momento es mayor o igual que el tamaño escalado máximo establecido, no se procesará
                    if ((scaleSize.x/imageSize.x >= originScale.x * maxScrole
                            || scaleSize.y/imageSize.y >= originScale.y * maxScrole)
                            && getDoubleFingerDistance(event) - doublePointDistance > 0){
                        break;
                    }
                    // Establezca aquí cuando el cambio de distancia del zoom con dos dedos sea superior a 50 y la corriente no esté en el estado de zoom con dos dedos, calcule el punto central y espere algunas operaciones
                    if (Math.abs(getDoubleFingerDistance(event) - doublePointDistance) > 50
                            && zoomInMode != ZoomMode.TowFingerZoom){
                        // Calcula el punto central entre los dos dedos como punto central del zoom
                        doublePointCenter.set((event.getX(0) + event.getX(1))/2,
                                (event.getY(0) + event.getY(1))/2);
                        // Se supone que el punto central de los dos dedos es el punto donde se hizo clic
                        clickPoint.set(doublePointCenter);
                        // Lo siguiente es básicamente lo mismo que hacer doble clic para acercar
                        getBitmapOffset();
                        // Registre la relación de la distancia entre el punto en el que se hizo clic y la esquina superior izquierda de la imagen en el eje x, y y la longitud lateral de la imagen en el eje x, y, respectivamente,
                        // Es conveniente calcular el punto de coordenadas correspondiente a este punto después de hacer zoom
                        tempPoint.set((clickPoint.x - bitmapOriginPoint.x)/scaleSize.x,
                                (clickPoint.y - bitmapOriginPoint.y)/scaleSize.y);
                        // Establecer para ingresar al estado de zoom con dos dedos
                        zoomInMode = ZoomMode.TowFingerZoom;
                    }
                    // Si ha entrado en el estado de zoom con dos dedos, calcule directamente la relación de zoom y realice el desplazamiento
                    if (zoomInMode == ZoomMode.TowFingerZoom){
                        // Multiplica la relación de zoom actual por la relación de zoom de la distancia entre los dos dedos en este momento para obtener la imagen correspondiente.
                        float scrole =
                                doubleFingerScrole*getDoubleFingerDistance(event)/doublePointDistance;
                        // Esto es lo mismo que cuando se hace doble clic para acercar
                        scaleImage(new PointF(scrole,scrole));
                        getBitmapOffset();
                        translationImage(
                                new PointF(
                                        clickPoint.x - (bitmapOriginPoint.x + tempPoint.x*scaleSize.x),
                                        clickPoint.y - (bitmapOriginPoint.y + tempPoint.y*scaleSize.y))
                        );
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // El evento se activa cuando se suelta el dedo
                Log.e("kzg","***********************ACTION_UP");
                lastFingerNum = 0;
                break;
        }
        return true;
    }



    public void scaleImage(PointF scaleXY){
        matrix.setScale(scaleXY.x,scaleXY.y);
        scaleSize.set(scaleXY.x * imageSize.x,scaleXY.y * imageSize.y);
        setImageMatrix(matrix);
    }

    /**
     * Traducir la imagen en las direcciones de los ejes X e Y
     * @param pointF
     */
    public void translationImage(PointF pointF){
        matrix.postTranslate(pointF.x,pointF.y);
        setImageMatrix(matrix);
    }


    /**
     * Evite que la imagen en movimiento exceda el límite y calcule la condición de límite
     * @param moveX
     * @param moveY
     * @return
     */
    public float[] moveBorderDistance(float moveX,float moveY){
        // Calcula las coordenadas de la esquina superior izquierda del mapa de bits
        getBitmapOffset();

        // Calcula las coordenadas de la esquina inferior derecha del mapa de bits
        float bitmapRightBottomX = bitmapOriginPoint.x + scaleSize.x;
        float bitmapRightBottomY = bitmapOriginPoint.y + scaleSize.y;

        if (moveY > 0){
            //Bajar deslizándose
            if (bitmapOriginPoint.y + moveY > 0){
                if (bitmapOriginPoint.y < 0){
                    moveY = -bitmapOriginPoint.y;
                }else {
                    moveY = 0;
                }
            }
        }else if (moveY < 0){
            //Deslizar hacia arriba
            if (bitmapRightBottomY + moveY < viewSize.y){
                if (bitmapRightBottomY > viewSize.y){
                    moveY = -(bitmapRightBottomY - viewSize.y);
                }else {
                    moveY = 0;
                }
            }
        }

        if (moveX > 0){
            //Desliza a la derecha
            if (bitmapOriginPoint.x + moveX > 0){
                if (bitmapOriginPoint.x < 0){
                    moveX = -bitmapOriginPoint.x;
                }else {
                    moveX = 0;
                }
            }
        }else if (moveX < 0){
            // Deslizar a la izquierda
            if (bitmapRightBottomX + moveX < viewSize.x){
                if (bitmapRightBottomX > viewSize.x){
                    moveX = -(bitmapRightBottomX - viewSize.x);
                }else {
                    moveX = 0;
                }
            }
        }
        return new float[]{moveX,moveY};
    }

    /**
     * Obtener el punto de coordenadas del mapa de bits en la vista
     */
    public void getBitmapOffset(){
        float[] value = new float[9];
        float[] offset = new float[2];
        Matrix imageMatrix = getImageMatrix();
        imageMatrix.getValues(value);
        offset[0] = value[2];
        offset[1] = value[5];
        bitmapOriginPoint.set(offset[0],offset[1]);
    }


    /**
     * Calcula la distancia entre cero dedos
     * @param event
     * @return
     */
    public static float  getDoubleFingerDistance(MotionEvent event){
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return  (float)Math.sqrt(x * x + y * y) ;
    }
}

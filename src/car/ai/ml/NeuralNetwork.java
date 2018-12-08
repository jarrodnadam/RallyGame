package car.ai.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.jme3.math.FastMath;

import helper.Log;

public class NeuralNetwork {
	int num_layers;
	int[] sizes;
	float[][] biases;
	float[][][] weights;
	
	public NeuralNetwork(int[] sizes) {
		this.num_layers = sizes.length;
		this.sizes = sizes;
		
		this.biases = new float[sizes.length - 1][];
		
		
		
		for (int i=0; i<sizes.length - 1; i++) {
			this.biases[i] = randomArray(sizes[i + 1]);
		}
		
		
		/*
		int i = 0;
		for (int x: colonMethod(sizes, 1, -1)) {
			this.biases[i] = randomArray(x);
			i++;
		}
		*/
		
		/*
		forEach(
				Arrays.stream(colonMethod(sizes, 1, -1))
					.boxed()
					.collect(Collectors.toList()), 
				(x, i, n) -> this.biases[i] = randomArray(x)
		);
		*/
		
		int[][] weightsShape = zipIntArray(sizes, colonMethod(sizes, 1, -1));
		
		this.weights = new float[weightsShape.length][][];
		
		
		forEach(
				Arrays.stream(weightsShape).collect(Collectors.toList()), 
				(x, i, n) -> this.weights[i] = randomMatrix(x[1], x[0])
		);
		
		//int[] temp = zipFloatArray(sizes, colonMethod(sizes, 1, -1));
		/*
		for (i=0; i<sizes.length; i++) {

			//this.biases[i] = zipIntArray(sizes, randomArray(sizes.length - 1);
		}*/
		
		List<Float> a = new LinkedList<Float>();
	}
	
	
	
	public float[] feedForward(float[] input) {
		// feeds the input array through the network until output
		// TODO last layer will be tanh [-1, 1]
		// TODO consider using softmax over sigmoid for obvious benefits
		
		float[][] b = this.biases;
		float[][][] w = this.weights;
		
		float[] output = input.clone();

		for (int i = 0 ; i < w.length-1; i++) {
			
			output = sigmoid(sumArray(dotProd(w[i], output), b[i]));
		
		}
		
		// to normalise the elements of output to sum to 1
		output = softmax(sumArray(dotProd(w[w.length-1], output), b[w.length-1]));
		return output;
	}
	
	
	public static float[] dotProd(float[][] matrix, float[] array) {
		
		float[] output = new float[matrix.length];
		
		for (int i = 0; i < matrix.length; i++) {
			float temp = 0;
			for (int j = 0; j < matrix[i].length; j++) {
				temp += matrix[i][j] * array[j];
			}
			output[i] = temp;
		}
		
		return output;
	}
	
	
	public static float[] softmax(float[] z) {
		
		float[] output = new float[z.length];
		float total = 0;
		for (int i = 0; i < z.length; i++) {
			output[i] = FastMath.exp(z[i]);
			total += output[i];
		}
		
		for (int i = 0; i < z.length; i++) {
			output[i] /= total;
		}
		
		return output;
	}

	
	public static float[] sigmoid(float[] z) {
		
		float[] output = new float[z.length];
		for (int i = 0; i < z.length; i++) {
			output[i] = 1.0f / (1.0f + FastMath.exp(-z[i]));
		}
		
		return output;
	}
	public static int[] sumArray(int[] a, int[] b) {
		
		int result[] = new int[a.length];
		Arrays.setAll(result, i -> a[i] + b[i]);
		return result;
	}
	
	public static float[] sumArray(float[] a, float[] b) {
		
		float[] result = new float[a.length];
		
		for (int i = 0; i < a.length; i++) {
			result[i] = a[i] + b[i];
		}
		return result;
	}
	
	
	public static float[] randomArray(int size) {
		float[] output = new float[size];
		
		for (int i=0; i<size; i++) {
			output[i] = (float)Math.random();
		}
		
		return output;
	}
	public static float[][] randomMatrix(int size1, int size2) {
		float[][] output = new float[size1][size2];
		
		for (int i=0; i<size1; i++) {
			output[i] = randomArray(size2);
		}
		
		return output;
	}
	
	
	
	public static int[] colonMethod(int[] arrayIn, int start, int end) {
		
		if (end == -1) {
			end = arrayIn.length - 1;
		}
		int[] temp = new int[end - start + 1];
		
		for (int i=0; i<temp.length; i++) {
			temp[i] = arrayIn[start + i];
		}
		return temp;
	}
	
	public static float[][] zipIntToFloatArray(int[] list1, int[] list2) {
		float[][] zipped;
		int zippedLength;
		
		if (list1.length < list2.length) {
			zippedLength = list2.length;
		} else {
			zippedLength = list1.length;
		}
		zipped = new float[zippedLength][2];
		
		for (int i=0; i<zippedLength; i++) {
			
			//= [list1[i], list2[i]];
			zipped[i] = new float[]{list1[i], list2[i]};
		}
		
		return zipped;
	}
	
	public static int[][] zipIntArray(int[] list1, int[] list2) {
		int[][] zipped;
		int zippedLength;
		zippedLength = Math.min(list1.length, list2.length);

		zipped = new int[zippedLength][2];
		
		for (int i=0; i<zippedLength; i++) {
			
			//= [list1[i], list2[i]];
			zipped[i] = new int[]{list1[i], list2[i]};
		}
		
		return zipped;
	}
	
	public static float[][] zipFloatArray(float[] list1, float[] list2) {
		float[][] zipped;
		int zippedLength;
		
		if (list1.length < list2.length) {
			zippedLength = list2.length;
		} else {
			zippedLength = list1.length;
		}
		zipped = new float[zippedLength][2];
		
		for (int i=0; i<zippedLength; i++) {
			
			//= [list1[i], list2[i]];
			zipped[i] = new float[]{list1[i], list2[i]};
		}
		
		return zipped;
	}
	
	public static <T> List<List<T>> zipList(List<T>... lists) {
	    List<List<T>> zipped = new ArrayList<>();
	    for (List<T> list : lists) {
	        for (int i = 0, listSize = list.size(); i < listSize; i++) {
	            List<T> list2;
	            if (i >= zipped.size())
	                zipped.add(list2 = new ArrayList<T>());
	            else
	                list2 = zipped.get(i);
	            list2.add(list.get(i));
	        }
	    }
	    return zipped;
	}
	
	
   @FunctionalInterface
   public interface LoopWithIndexAndSizeConsumer<T> {
       void accept(T t, int i, int n);
   }
   public static <T> void forEach(Collection<T> collection,
                                  LoopWithIndexAndSizeConsumer<T> consumer) {
      int index = 0;
      for (T object : collection){
         consumer.accept(object, index++, collection.size());
      }
   }

	public static void main(String[] args) {
		int[] sizes = new int[]{3, 2, 4};

		float[] test_data = new float[]{1.0f, 2.0f, 3.0f};		
		
		NeuralNetwork NN = new NeuralNetwork(sizes);
		float[] output = NN.feedForward(test_data);
		Log.p(output, ",");
	}
	
}

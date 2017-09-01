package org.tfelab.txt;

public class NeedlemanWunsch {
	char[] mSeqA;
	char[] mSeqB;
	int[][] mD;
	int mScore;
	String mAlignmentSeqA = "";
	String mAlignmentSeqB = "";

	void init(char[] seqA, char[] seqB) {
		this.mSeqA = seqA;
		this.mSeqB = seqB;
		this.mD = new int[this.mSeqA.length + 1][this.mSeqB.length + 1];
		for (int i = 0; i <= this.mSeqA.length; i++) {
			for (int j = 0; j <= this.mSeqB.length; j++) {
				if (i == 0) {
					this.mD[i][j] = -j;
				} else if (j == 0) {
					this.mD[i][j] = -i;
				} else {
					this.mD[i][j] = 0;
				}
			}
		}
	}

	void process() {
		for (int i = 1; i <= this.mSeqA.length; i++) {
			for (int j = 1; j <= this.mSeqB.length; j++) {
				int scoreDiag = this.mD[i - 1][j - 1] + weight(i, j);
				int scoreLeft = this.mD[i][j - 1] - 1;
				int scoreUp = this.mD[i - 1][j] - 1;
				this.mD[i][j] = Math.max(Math.max(scoreDiag, scoreLeft),
						scoreUp);
			}
		}
	}

	void backtrack() {
		int i = this.mSeqA.length;
		int j = this.mSeqB.length;
		this.mScore = this.mD[i][j];
		while (i > 0 && j > 0) {
			if (this.mD[i][j] == this.mD[i - 1][j - 1] + weight(i, j)) {
				this.mAlignmentSeqA += this.mSeqA[i - 1];
				this.mAlignmentSeqB += this.mSeqB[j - 1];
				i--;
				j--;
				continue;
			} else if (this.mD[i][j] == this.mD[i][j - 1] - 1) {
				this.mAlignmentSeqA += "-";
				this.mAlignmentSeqB += this.mSeqB[j - 1];
				j--;
				continue;
			} else {
				this.mAlignmentSeqA += this.mSeqA[i - 1];
				this.mAlignmentSeqB += "-";
				i--;
				continue;
			}
		}
		this.mAlignmentSeqA = new StringBuffer(this.mAlignmentSeqA).reverse()
				.toString();
		this.mAlignmentSeqB = new StringBuffer(this.mAlignmentSeqB).reverse()
				.toString();
	}

	private int weight(int i, int j) {
		if (this.mSeqA[i - 1] == this.mSeqB[j - 1]) {
			return 1;
		} else {
			return -1;
		}
	}

	void printMatrix() {
		System.out.println("D =");
		for (int i = 0; i < this.mSeqA.length + 1; i++) {
			for (int j = 0; j < this.mSeqB.length + 1; j++) {
				System.out.print(String.format("%4d ", this.mD[i][j]));
			}
			System.out.println();
		}
		System.out.println();
	}

	void printScoreAndAlignments() {
		System.out.println("Score: " + this.mScore);
		System.out.println("Sequence A: " + this.mAlignmentSeqA);
		System.out.println("Sequence B: " + this.mAlignmentSeqB);
		System.out.println();
	}

	public static void main(String[] args) {
		char[] seqA = "苹果 iPhone6 金色 64G 国行 自用苹果6，金色，国行，64G".toCharArray();
		char[] seqB = "iPhone 6 A1586 公开版".toCharArray();

		NeedlemanWunsch nw = new NeedlemanWunsch();
		nw.init(seqA, seqB);
		nw.process();
		nw.backtrack();

		nw.printMatrix();
		nw.printScoreAndAlignments();
	}
}
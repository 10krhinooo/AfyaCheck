# Create a diagnostic script: check_model.py
import os
import pickle

def check_model_file():
    model_path = 'xgboost_non_imputed.pkl'

    print(f"Checking model file: {model_path}")
    print(f"File exists: {os.path.exists(model_path)}")

    if os.path.exists(model_path):
        file_size = os.path.getsize(model_path)
        print(f"File size: {file_size} bytes")

        if file_size == 0:
            print("❌ ERROR: File is empty (0 bytes)")
            return False

        try:
            with open(model_path, 'rb') as f:
                # Try to read the file
                content = f.read()
                print(f"Successfully read {len(content)} bytes")

            # Try to load with pickle
            with open(model_path, 'rb') as f:
                model_data = pickle.load(f)
                print("✅ Successfully loaded with pickle")
                print(f"Loaded data type: {type(model_data)}")

                if isinstance(model_data, dict):
                    print(f"Dictionary keys: {list(model_data.keys())}")
                    if 'model' in model_data:
                        model = model_data['model']
                        print(f"Model type: {type(model)}")
                        print(f"Model attributes: {dir(model)[:10]}")  # First 10 attributes
                else:
                    print(f"Direct model type: {type(model_data)}")

            return True

        except Exception as e:
            print(f"❌ Error loading file: {e}")
            return False
    else:
        print("❌ File does not exist")
        return False

if __name__ == "__main__":
    check_model_file()